import AppKit
import Foundation

let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let outputDir = root.appendingPathComponent("store-assets/google-play")
let featurePath = outputDir.appendingPathComponent("feature-graphic-1024x500.png")
let iconPath = outputDir.appendingPathComponent("icon-512.png")

try FileManager.default.createDirectory(at: outputDir, withIntermediateDirectories: true)

let size = NSSize(width: 1024, height: 500)
let image = NSImage(size: size)

func color(_ red: CGFloat, _ green: CGFloat, _ blue: CGFloat, _ alpha: CGFloat = 1) -> NSColor {
    NSColor(calibratedRed: red / 255, green: green / 255, blue: blue / 255, alpha: alpha)
}

func drawRoundedRect(_ rect: NSRect, radius: CGFloat, fill: NSColor, stroke: NSColor? = nil, lineWidth: CGFloat = 1) {
    let path = NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius)
    fill.setFill()
    path.fill()
    if let stroke {
        stroke.setStroke()
        path.lineWidth = lineWidth
        path.stroke()
    }
}

func drawText(_ text: String, rect: NSRect, font: NSFont, color: NSColor, paragraph: NSParagraphStyle? = nil) {
    let style = paragraph ?? {
        let p = NSMutableParagraphStyle()
        p.lineBreakMode = .byWordWrapping
        return p
    }()
    let attributes: [NSAttributedString.Key: Any] = [
        .font: font,
        .foregroundColor: color,
        .paragraphStyle: style
    ]
    (text as NSString).draw(in: rect, withAttributes: attributes)
}

image.lockFocus()

color(255, 248, 236).setFill()
NSRect(origin: .zero, size: size).fill()

for i in 0..<9 {
    let y = CGFloat(i) * 62 - 40
    let path = NSBezierPath()
    path.move(to: NSPoint(x: 0, y: y))
    path.curve(to: NSPoint(x: 1024, y: y + 30),
               controlPoint1: NSPoint(x: 260, y: y + 70),
               controlPoint2: NSPoint(x: 760, y: y - 55))
    color(235, 96, 76, i % 2 == 0 ? 0.07 : 0.045).setStroke()
    path.lineWidth = 3
    path.stroke()
}

drawRoundedRect(
    NSRect(x: 52, y: 54, width: 920, height: 392),
    radius: 34,
    fill: color(255, 255, 255, 0.86),
    stroke: color(230, 106, 88),
    lineWidth: 5
)

if let icon = NSImage(contentsOf: iconPath) {
    let iconRect = NSRect(x: 706, y: 146, width: 220, height: 220)
    drawRoundedRect(NSRect(x: 684, y: 124, width: 264, height: 264), radius: 42, fill: color(255, 240, 220), stroke: color(130, 184, 160), lineWidth: 5)
    icon.draw(in: iconRect, from: .zero, operation: .sourceOver, fraction: 1)
}

let titleFont = NSFont.systemFont(ofSize: 78, weight: .heavy)
let subtitleFont = NSFont.systemFont(ofSize: 34, weight: .semibold)
let bodyFont = NSFont.systemFont(ofSize: 26, weight: .medium)

drawText("밥크로스", rect: NSRect(x: 102, y: 272, width: 560, height: 92), font: titleFont, color: color(33, 62, 49))
drawText("가까운 밥친구와 메뉴를 정합니다", rect: NSRect(x: 106, y: 226, width: 560, height: 48), font: subtitleFont, color: color(79, 102, 88))
drawText("후보 올리기 · 같이 고르기 · 결과 공유", rect: NSRect(x: 108, y: 176, width: 560, height: 42), font: bodyFont, color: color(211, 56, 36))

drawRoundedRect(NSRect(x: 108, y: 116, width: 360, height: 54), radius: 27, fill: color(232, 247, 239), stroke: color(122, 180, 154), lineWidth: 2)
drawText("서버 없이 근거리 밥판", rect: NSRect(x: 142, y: 126, width: 300, height: 34), font: NSFont.systemFont(ofSize: 24, weight: .bold), color: color(33, 87, 66))

image.unlockFocus()

guard let tiff = image.tiffRepresentation,
      let bitmap = NSBitmapImageRep(data: tiff),
      let png = bitmap.representation(using: .png, properties: [:]) else {
    fatalError("Failed to render feature graphic")
}

try png.write(to: featurePath)
print(featurePath.path)
