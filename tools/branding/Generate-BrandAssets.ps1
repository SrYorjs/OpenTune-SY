param([string]$Root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path)
$ErrorActionPreference = 'Stop'
$Root = [System.IO.Path]::GetFullPath($Root)
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.IO;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;
public static class BrandAssets04 {
    static string Inside(string root, string relative) {
        string path = Path.GetFullPath(Path.Combine(root, relative));
        if (!path.StartsWith(root.TrimEnd('\\','/') + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase))
            throw new ArgumentException("Output must stay inside the workspace");
        return path;
    }
    public static void Render(string root, string input, string output, int width, int height, string mode) {
        string target = Inside(root, output);
        Directory.CreateDirectory(Path.GetDirectoryName(target));
        if (mode == "exact") { File.Copy(Inside(root, input), target, true); return; }
        using (Bitmap destination = new Bitmap(width, height, PixelFormat.Format32bppArgb)) {
            using (Graphics g = Graphics.FromImage(destination)) {
                g.Clear(Color.Transparent);
                g.CompositingMode = CompositingMode.SourceCopy;
                g.CompositingQuality = CompositingQuality.HighQuality;
                g.InterpolationMode = InterpolationMode.HighQualityBicubic;
                g.PixelOffsetMode = PixelOffsetMode.HighQuality;
                if (mode != "transparent") using (Bitmap source = new Bitmap(Inside(root, input))) {
                    RectangleF rect;
                    if (mode == "splash") {
                        float unit = width / 235f;
                        rect = new RectangleF(58f * unit, 48f * unit, 128f * unit, 128f * unit);
                    } else {
                        // Fit every nontransparent source pixel inside the circular safe zone.
                        BitmapData pixels = source.LockBits(new Rectangle(0,0,source.Width,source.Height), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
                        byte[] bytes = new byte[Math.Abs(pixels.Stride) * source.Height];
                        Marshal.Copy(pixels.Scan0, bytes, 0, bytes.Length);
                        double radius = 1;
                        for (int y=0; y<source.Height; y++) for (int x=0; x<source.Width; x++) {
                            if (bytes[y*Math.Abs(pixels.Stride)+x*4+3] == 0) continue;
                            double dx=x+.5-source.Width/2.0, dy=y+.5-source.Height/2.0;
                            radius=Math.Max(radius,Math.Sqrt(dx*dx+dy*dy));
                        }
                        source.UnlockBits(pixels);
                        float diameter = mode.StartsWith("adaptive") ? 66f/108f : mode == "notification" ? 20f/24f : .90f;
                        float scale = (float)(Math.Min(width,height)*diameter/2/radius);
                        rect = new RectangleF((width-source.Width*scale)/2,(height-source.Height*scale)/2,source.Width*scale,source.Height*scale);
                    }
                    using (ImageAttributes attributes = new ImageAttributes()) {
                        attributes.SetWrapMode(WrapMode.TileFlipXY);
                        if (mode.Contains("white") || mode == "notification") {
                            attributes.SetColorMatrix(new ColorMatrix(new float[][] {
                                new float[]{0,0,0,0,0},new float[]{0,0,0,0,0},new float[]{0,0,0,0,0},
                                new float[]{0,0,0,1,0},new float[]{1,1,1,0,1}
                            }));
                        }
                        g.DrawImage(source, Rectangle.Round(rect), 0,0,source.Width,source.Height,GraphicsUnit.Pixel,attributes);
                    }
                }
            }
            // GDI+ can round white to 254 on translucent resampled edges.
            // Preserve alpha and normalize the notification/themed mask RGB.
            if (mode.Contains("white") || mode == "notification") {
                BitmapData mask = destination.LockBits(new Rectangle(0,0,width,height), ImageLockMode.ReadWrite, PixelFormat.Format32bppArgb);
                byte[] bytes = new byte[Math.Abs(mask.Stride)*height];
                Marshal.Copy(mask.Scan0, bytes, 0, bytes.Length);
                for (int y=0; y<height; y++) for (int x=0; x<width; x++) {
                    int offset=y*Math.Abs(mask.Stride)+x*4;
                    bytes[offset]=bytes[offset+1]=bytes[offset+2]=255;
                }
                Marshal.Copy(bytes, 0, mask.Scan0, bytes.Length);
                destination.UnlockBits(mask);
            }
            destination.Save(target, ImageFormat.Png);
        }
    }
}
'@
$manifestPath = Join-Path $Root 'docs/inventario-04b/branding.json'
$plan = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
foreach ($job in $plan.outputs) {
    [BrandAssets04]::Render($Root, $job.input, $job.output, $job.width, $job.height, $job.mode)
}
Write-Output "Generated $($plan.outputs.Count) PNG assets inside $Root"
