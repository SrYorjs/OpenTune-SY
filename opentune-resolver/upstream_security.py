import asyncio
import ipaddress
import socket

import httpx
from httpcore._backends.auto import AutoBackend
from httpcore._backends.base import AsyncNetworkBackend


async def public_addresses(host: str, port: int) -> list[str]:
    try:
        records = await asyncio.get_running_loop().getaddrinfo(host, port, type=socket.SOCK_STREAM)
    except OSError as error:
        raise httpx.ConnectError("Upstream DNS unavailable") from error
    addresses = list(dict.fromkeys(row[4][0] for row in records))
    if not addresses:
        raise httpx.ConnectError("Upstream DNS unavailable")
    for value in addresses:
        address = ipaddress.ip_address(value)
        mapped = getattr(address, "ipv4_mapped", None)
        if not address.is_global or address.is_multicast or address.is_reserved or address.is_unspecified or getattr(address,"is_site_local",False) or (mapped is not None and not mapped.is_global):
            raise httpx.ConnectError("Upstream address is not public")
    return addresses


class PublicNetworkBackend(AsyncNetworkBackend):
    def __init__(self):
        self.backend = AutoBackend()

    async def connect_tcp(self, host, port, timeout=None, local_address=None, socket_options=None):
        addresses = await public_addresses(host, port)
        last_error = None
        for address in addresses:
            try:
                return await self.backend.connect_tcp(address, port, timeout, local_address, socket_options)
            except Exception as error:
                last_error = error
        raise last_error

    async def connect_unix_socket(self, path, timeout=None, socket_options=None):
        raise httpx.ConnectError("Unix upstreams are not allowed")

    async def sleep(self, seconds):
        await asyncio.sleep(seconds)


def upstream_transport(proxy=None):
    transport = httpx.AsyncHTTPTransport(proxy=proxy, retries=0, limits=httpx.Limits(max_connections=12, max_keepalive_connections=6))
    transport._pool._network_backend = PublicNetworkBackend()
    return transport
