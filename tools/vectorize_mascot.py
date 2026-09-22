import base64
import io
import math
import re
from collections import defaultdict, deque
from pathlib import Path

import numpy as np
from PIL import Image


SOURCE = Path(r"C:\Users\rogeriosilva-ieg\.codex\generated_images\01a08b85-3ac8-7da0-b7f5-1409e6bb23c2\exec-7ab10be6-682f-484e-bd97-02925cfcc8f8.png")
OUTPUT_DIR = Path(r"C:\Users\rogeriosilva-ieg\.codex\visualizations\2026\09\10\01a08b85-3ac8-7da0-b7f5-1409e6bb23c2")
PNG_OUT = OUTPUT_DIR / "mascotinho_cutout.png"
SVG_OUT = OUTPUT_DIR / "mascotinho_vector.svg"


def rdp(points, epsilon):
    if len(points) < 3:
        return points
    a = np.asarray(points[0], dtype=float)
    b = np.asarray(points[-1], dtype=float)
    p = np.asarray(points[1:-1], dtype=float)
    ab = b - a
    denom = float(np.dot(ab, ab))
    if denom == 0:
        d = np.linalg.norm(p - a, axis=1)
    else:
        t = np.clip(((p - a) @ ab) / denom, 0, 1)
        proj = a + t[:, None] * ab
        d = np.linalg.norm(p - proj, axis=1)
    idx = int(np.argmax(d)) if len(d) else 0
    maximum = float(d[idx]) if len(d) else 0.0
    if maximum > epsilon:
        split = idx + 1
        left = rdp(points[: split + 1], epsilon)
        right = rdp(points[split:], epsilon)
        return left[:-1] + right
    return [points[0], points[-1]]


def simplify_closed(loop, epsilon=1.25):
    if len(loop) < 5:
        return loop
    pts = loop[:-1] if loop[0] == loop[-1] else loop[:]
    # Start at a stable extremity, then split the closed contour at the farthest point.
    start_i = min(range(len(pts)), key=lambda i: (pts[i][0], pts[i][1]))
    pts = pts[start_i:] + pts[:start_i]
    sx, sy = pts[0]
    far_i = max(range(1, len(pts)), key=lambda i: (pts[i][0] - sx) ** 2 + (pts[i][1] - sy) ** 2)
    first = rdp(pts[: far_i + 1], epsilon)
    second = rdp(pts[far_i:] + [pts[0]], epsilon)
    out = first[:-1] + second[:-1]
    if len(out) < 3:
        return loop
    out.append(out[0])
    return out


def connected_components(mask):
    h, w = mask.shape
    seen = np.zeros_like(mask, dtype=bool)
    for sy, sx in zip(*np.nonzero(mask)):
        if seen[sy, sx]:
            continue
        queue = deque([(int(sx), int(sy))])
        seen[sy, sx] = True
        pixels = []
        while queue:
            x, y = queue.popleft()
            pixels.append((x, y))
            for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                if 0 <= nx < w and 0 <= ny < h and mask[ny, nx] and not seen[ny, nx]:
                    seen[ny, nx] = True
                    queue.append((nx, ny))
        yield pixels


def trace_component(pixels):
    pixel_set = set(pixels)
    outgoing = defaultdict(list)
    for x, y in pixels:
        if (x, y - 1) not in pixel_set:
            outgoing[(x, y)].append((x + 1, y))
        if (x + 1, y) not in pixel_set:
            outgoing[(x + 1, y)].append((x + 1, y + 1))
        if (x, y + 1) not in pixel_set:
            outgoing[(x + 1, y + 1)].append((x, y + 1))
        if (x - 1, y) not in pixel_set:
            outgoing[(x, y + 1)].append((x, y))

    unused = {(a, b) for a, ends in outgoing.items() for b in ends}
    loops = []
    dir_idx = {(1, 0): 0, (0, 1): 1, (-1, 0): 2, (0, -1): 3}
    while unused:
        start_edge = next(iter(unused))
        start, current = start_edge
        unused.remove(start_edge)
        loop = [start, current]
        previous = start
        guard = 0
        while current != start and guard < len(unused) + 10000:
            guard += 1
            candidates = [n for n in outgoing.get(current, []) if (current, n) in unused]
            if not candidates:
                break
            incoming = dir_idx[(current[0] - previous[0], current[1] - previous[1])]
            priority = [(incoming + 1) % 4, incoming, (incoming + 3) % 4, (incoming + 2) % 4]
            by_dir = {dir_idx[(n[0] - current[0], n[1] - current[1])]: n for n in candidates}
            nxt = next(by_dir[d] for d in priority if d in by_dir)
            unused.remove((current, nxt))
            previous, current = current, nxt
            loop.append(current)
        if current == start and len(loop) >= 5:
            loops.append(loop)
    return loops


def path_data(loop):
    pts = simplify_closed(loop)
    parts = [f"M{pts[0][0]} {pts[0][1]}"]
    for x, y in pts[1:-1]:
        parts.append(f"L{x} {y}")
    parts.append("Z")
    return "".join(parts)


def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    if SOURCE.suffix.lower() == ".svg":
        raw = SOURCE.read_text(encoding="utf-8")
        match = re.search(r"data:image/png;base64,([^\"']+)", raw)
        if not match:
            raise SystemExit("Embedded PNG not found")
        source = Image.open(io.BytesIO(base64.b64decode(match.group(1)))).convert("RGBA")
    else:
        source = Image.open(SOURCE).convert("RGBA")
    source.save(PNG_OUT)

    # The wrapper's native size keeps the path count practical while retaining its silhouette.
    source.thumbnail((240, 211), Image.Resampling.LANCZOS)
    rgba = np.array(source)
    alpha = rgba[:, :, 3]
    visible = alpha >= 36
    rgb_image = Image.fromarray(rgba[:, :, :3], "RGB")
    palette = rgb_image.quantize(colors=12, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    indices = np.array(palette)
    colors = palette.getpalette()

    # Tiny isolated specks come from antialiasing and create needless nodes.
    paths = []
    for index in np.unique(indices[visible]):
        mask = (indices == index) & visible
        if int(mask.sum()) < 4:
            continue
        off = int(index) * 3
        color = f"#{colors[off]:02x}{colors[off + 1]:02x}{colors[off + 2]:02x}"
        for component in connected_components(mask):
            if len(component) < 7:
                continue
            pieces = []
            for loop in trace_component(component):
                area2 = abs(sum(loop[i][0] * loop[i + 1][1] - loop[i + 1][0] * loop[i][1] for i in range(len(loop) - 1)))
                if area2 >= 6:
                    pieces.append(path_data(loop))
            if pieces:
                paths.append((len(component), f'<path fill="{color}" stroke="{color}" stroke-width="0.55" stroke-linejoin="round" fill-rule="evenodd" d="{"".join(pieces)}"/>'))

    # Larger areas first; details and highlights are painted last.
    paths.sort(reverse=True, key=lambda item: item[0])
    w, h = source.size
    silhouette_parts = []
    for component in connected_components(visible):
        if len(component) < 7:
            continue
        silhouette_parts.extend(path_data(loop) for loop in trace_component(component))
    silhouette = (
        f'<path fill="#fff5d7" stroke="#fff5d7" stroke-width="0.8" stroke-linejoin="round" '
        f'fill-rule="evenodd" d="{"".join(silhouette_parts)}"/>'
    )
    svg = (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">'
        + silhouette
        + "".join(p for _, p in paths)
        + "</svg>"
    )
    SVG_OUT.write_text(svg, encoding="utf-8")
    print({
        "source_size": Image.open(PNG_OUT).size,
        "vector_size": source.size,
        "alpha_corner": int(alpha[0, 0]),
        "colors": len(paths),
        "svg_bytes": SVG_OUT.stat().st_size,
        "png": str(PNG_OUT),
        "svg": str(SVG_OUT),
    })


if __name__ == "__main__":
    main()
