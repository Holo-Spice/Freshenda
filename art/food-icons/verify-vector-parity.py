"""确认 SVG 与 VectorDrawable 的路径、颜色、笔画和分组变换完全一致。"""
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
A = '{http://schemas.android.com/apk/res/android}'
manifest = json.loads((HERE / 'manifest.json').read_text(encoding='utf-8'))


def flatten(element, android=False, transforms=()):
    result = []
    for child in element:
        tag = child.tag.rsplit('}', 1)[-1]
        if tag in ('g', 'group'):
            if android:
                values = tuple(float(child.get(A + k, default)) for k, default in (
                    ('translateX', '0'), ('translateY', '0'), ('rotation', '0'),
                    ('scaleX', '1'), ('scaleY', '1')))
            else:
                values = tuple(map(float, re.findall(r'-?\d+(?:\.\d+)?', child.get('transform', ''))))
            result.extend(flatten(child, android, transforms + (values,)))
        elif tag == 'path':
            def attr(svg, vector, default):
                value = child.get(A + vector if android else svg, default)
                return '#00000000' if value == 'none' else value
            result.append((transforms, attr('d', 'pathData', ''),
                           attr('fill', 'fillColor', '#00000000'),
                           attr('stroke', 'strokeColor', '#00000000'),
                           float(attr('stroke-width', 'strokeWidth', '0')),
                           attr('stroke-linecap', 'strokeLineCap', ''),
                           attr('stroke-linejoin', 'strokeLineJoin', '')))
        else:
            raise AssertionError(f'Unsupported element: {tag}')
    return result


for item in manifest['items']:
    key = item['iconKey']
    svg = ET.parse(HERE / f'{key}.svg').getroot()
    vector = ET.parse(ROOT / item['drawable']).getroot()
    assert svg.get('viewBox') == '0 0 96 96', key
    assert vector.get(A + 'viewportWidth') == vector.get(A + 'viewportHeight') == '96', key
    assert flatten(svg) == flatten(vector, android=True), f'路径或属性不一致: {key}'
print(f"PASS: {len(manifest['items'])} SVG / VectorDrawable pairs match exactly.")
