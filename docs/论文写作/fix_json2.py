import json, os

BASE = r"D:\Office-software\project\springboot+vue"

def fix_embedded_quotes(fname):
    fpath = os.path.join(BASE, fname)
    with open(fpath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Save backup
    with open(fpath + '.bak', 'w', encoding='utf-8') as f:
        f.write(content)

    # Find all "text": lines and fix embedded double quotes
    # The issue: Chinese-style quoted terms like "约定优于配置" contain
    # U+201C (") and U+201D (") which look like ASCII " in some editors
    # but are actually valid Unicode. Check what's really breaking.

    lines = content.split('\n')
    fixed_lines = []
    fixed_count = 0

    for line in lines:
        # Check if this is a "text" value line
        if '"text":' in line:
            # Find the value part between outer quotes
            # Pattern: spaces + "text": "VALUE",
            prefix_end = line.index('"text":') + len('"text":')
            # Skip whitespace and opening quote
            rest = line[prefix_end:].lstrip()
            if rest.startswith('"'):
                # Value starts after the opening quote
                val_start = prefix_end + (len(line[prefix_end:]) - len(rest)) + 1  # +1 for opening "
                # Find the closing quote (last non-comma quote)
                # Simple: find last '"' before optional comma
                stripped = line.rstrip()
                # Remove trailing comma if present
                has_comma = stripped.endswith(',')
                if has_comma:
                    stripped = stripped[:-1]
                # Find the last " which should be the closing quote
                last_quote = stripped.rfind('"')
                if last_quote > val_start:
                    val_end = last_quote

                    prefix = line[:val_start]
                    value = line[val_start:val_end]
                    suffix = line[val_end:]

                    # Check for problematic characters in value
                    # Replace ASCII double quotes inside value with Chinese quotes
                    if '"' in value:
                        # Find unescaped double quotes
                        # In Chinese text, we want to replace them with fullwidth versions
                        new_value = value.replace('"', '“').replace('“', '“', 1)
                        # Actually, let's just replace all " inside with “” alternating
                        parts = value.split('"')
                        new_value = parts[0]
                        for i, part in enumerate(parts[1:], 1):
                            if i % 2 == 1:
                                new_value += '“' + part
                            else:
                                new_value += '”' + part

                        line = prefix + new_value + suffix
                        fixed_count += 1
        fixed_lines.append(line)

    new_content = '\n'.join(fixed_lines)

    # Validate
    try:
        data = json.loads(new_content)
        with open(fpath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Fixed {fname}: {fixed_count} lines repaired, {len(data)} paragraphs")
        return True
    except json.JSONDecodeError as e:
        print(f"FAILED {fname}: still broken at line {e.lineno}")
        # Show context
        line = new_content.split('\n')[e.lineno - 1]
        print(f"  {line[max(0,e.colno-20):e.colno+20]}")
        return False

fix_embedded_quotes(r"docs\论文写作\extracted_ch2_tech_humanized.json")
fix_embedded_quotes(r"docs\论文写作\extracted_ch5_impl_humanized.json")
