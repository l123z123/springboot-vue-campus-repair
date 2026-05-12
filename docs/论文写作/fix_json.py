import json, os, sys

BASE = r"D:\Office-software\project\springboot+vue"
FILES = [
    "docs/论文写作/extracted_ch2_tech_humanized.json",
    "docs/论文写作/extracted_ch5_impl_humanized.json",
]

def fix_json_embedded_quotes(filepath):
    """Fix JSON files where Chinese double-quotes inside string values
    were written as ASCII double quotes, breaking JSON parsing."""
    fpath = os.path.join(BASE, filepath)

    with open(fpath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Make backup
    with open(fpath + '.bak', 'w', encoding='utf-8') as f:
        f.write(content)

    # Try original parse
    error = None
    try:
        data = json.loads(content)
        print(f"{filepath}: Already valid ({len(data)} items)")
        return
    except json.JSONDecodeError as e:
        error = e
        print(f"{filepath}: Error line {e.lineno} col {e.colno}: {e.msg}")

    # Show problem character
    lines = content.split('\n')
    line = lines[error.lineno - 1]
    pos = error.colno - 1
    ch = line[pos]
    print(f"  Char at col {error.colno}: U+{ord(ch):04X} ({repr(ch)})")

    # Fix: replace ASCII double quotes inside text values with Chinese quotes
    # Strategy: process line by line, fixing text values
    import re

    fixed_lines = []
    for line in lines:
        # Match "text": "..." pattern in JSON
        m = re.match(r'^(\s*"text":\s*")(.*)(")\s*$', line)
        if m:
            prefix = m.group(1)
            body = m.group(2)
            suffix = m.group(3)
            # Replace ASCII double quotes inside body with Chinese fullwidth quotes
            body = body.replace('“', '『')  # " → 『
            body = body.replace('”', '』')  # " → 』
            # Also handle literal ASCII " that might be embedded
            # (but be careful not to break escaped \")
            line = prefix + body + suffix
        fixed_lines.append(line)

    fixed_content = '\n'.join(fixed_lines)

    try:
        data = json.loads(fixed_content)
        with open(fpath, 'w', encoding='utf-8') as f:
            f.write(fixed_content)
        print(f"  Fixed! Now valid ({len(data)} items)")
    except json.JSONDecodeError as e2:
        print(f"  Still broken after fix: {e2}")
        # Try more aggressive fix
        print("  Trying aggressive fix...")
        # Scan character by character to find all embedded quote issues
        # Show all spots where we have " inside a text value
        fixed2 = fix_text_values(content)
        if fixed2:
            try:
                data = json.loads(fixed2)
                with open(fpath, 'w', encoding='utf-8') as f:
                    f.write(fixed2)
                print(f"  Aggressive fix succeeded! ({len(data)} items)")
            except json.JSONDecodeError as e3:
                print(f"  Aggressive fix also failed: {e3}")

def fix_text_values(content):
    """More aggressive fix: process the JSON structure manually."""
    lines = content.split('\n')
    fixed_lines = []

    for line in lines:
        stripped = line.strip()
        # Find lines that have "text": "..."
        if '"text":' in stripped:
            # Find the start of the text value
            idx = line.find('"text":')
            prefix_end = line.find('"', line.find('"', idx + 8) + 1)  # find opening quote of value

            # Actually, let me do this more carefully
            # Find the pattern: "text": "VALUE",
            # We need to ensure VALUE doesn't contain unescaped "

            # Simple approach: for the value between the outer quotes,
            # escape any " characters that appear
            import re
            m = re.match(r'^(\s*"text":\s*)"(.*)"(\s*,?\s*)$', line)
            if m:
                prefix = m.group(1) + '"'
                body = m.group(2)
                suffix = '"' + m.group(3)

                # Replace any " in body with Chinese angle quotes
                # First check if body already has escaped quotes
                body_fixed = body
                # Replace remaining raw double-quotes
                body_fixed = body_fixed.replace('"', '“')
                body_fixed = body_fixed.replace('"', '”')
                # Also unescape any \\\" patterns
                body_fixed = body_fixed.replace('\\"', '"')

                line = prefix + body_fixed + suffix
        fixed_lines.append(line)

    return '\n'.join(fixed_lines)

for f in FILES:
    fix_json_embedded_quotes(f)
    print()
