"""Regenerate ch5 humanized JSON from original clean data using regex patterns."""
import json, re, os

BASE = r"D:\Office-software\project\springboot+vue"

# Read original clean JSON
orig_path = os.path.join(BASE, r"docs\论文写作\extracted_ch5_impl.json")
with open(orig_path, 'r', encoding='utf-8') as f:
    items = json.load(f)

print(f"Loaded {len(items)} paragraphs from original")

# Humanizer-ZH pattern replacements (ordered, applied sequentially)
REPLACEMENTS = [
    # Remove qualifier words
    (r'至关重要的', ''),
    (r'关键的', '重要的'),
    (r'核心的', ''),
    (r'全面的', ''),
    (r'完善的', ''),
    (r'强大的', ''),
    (r'深刻的', ''),
    (r'丰富的', ''),
    (r'显著的', '明显的'),

    # Remove filler phrases
    (r'值得注意的是，', ''),
    (r'可以看出，', ''),
    (r'由此可见，', ''),
    (r'综上所述，', ''),

    # Simplify AI vocabulary
    (r'此外，', '系统还'),
    (r'彰显', '显示'),
    (r'标志', ''),

    # Fix over-qualified patterns
    (r'为后续的功能扩展奠定了良好的基础', '便于后续扩展'),
    (r'奠定了技术基础', '提供了基础'),
    (r'发挥着重要作用', '起到重要作用'),

    # Remove over-explaining
    (r'，负载越低，维修工越有空闲处理新工单', ''),
    (r'，经验越丰富加分越多', ''),
    (r'，熟悉校园区域的维修工对位置和环境更了解', ''),
    (r'，平均分越高说明服务质量越好', ''),

    # Fix pattern: 通过...实现...
    (r'通过([^，]+)实现', r'使用\1完成'),

    # Fix promotional language
    (r'显著提升', '提升'),
    (r'大幅减少', '减少'),
    (r'极大提高', '提高'),
]

def apply_humanizer(text):
    """Apply humanizer-ZH patterns to text."""
    result = text

    # Apply regex replacements
    for pattern, replacement in REPLACEMENTS:
        result = re.sub(pattern, replacement, result)

    # Remove duplicate spaces
    result = re.sub(r'  +', ' ', result)

    # Fix pattern: "不仅...而且..." -> break into simpler
    result = re.sub(r'不仅([^，,]+)，而且([^，,。]+)', r'\1，\2', result)

    return result

# Apply to all items
changed = 0
for item in items:
    old_text = item['text']
    new_text = apply_humanizer(old_text)
    if old_text != new_text:
        item['text'] = new_text
        changed += 1

print(f"Changed {changed} paragraphs")

# Write valid JSON
out_path = os.path.join(BASE, r"docs\论文写作\extracted_ch5_impl_humanized.json")
with open(out_path, 'w', encoding='utf-8') as f:
    json.dump(items, f, ensure_ascii=False, indent=2)

print(f"Saved to extracted_ch5_impl_humanized.json")

# Validate
with open(out_path, 'r', encoding='utf-8') as f:
    data = json.load(f)
print(f"Validated: {len(data)} items")
