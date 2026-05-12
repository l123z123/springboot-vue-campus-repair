import json, sys, re

def fix_json(fpath):
    with open(fpath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Strategy: use a simple state machine to find all "text" values
    # and escape any unescaped double quotes within them

    result = []
    i = 0
    while i < len(content):
        # Look for "text":
        rest = content[i:]
        if rest.startswith('"text":'):
            # Find the value part
            # Structure: "text": "VALUE",
            idx = content.index('"text":', i)
            # Find the opening quote of the value
            after_key = content[idx + len('"text":'):]
            # Skip whitespace
            j = 0
            while j < len(after_key) and after_key[j] in ' \t':
                j += 1
            if j < len(after_key) and after_key[j] == '"':
                # opening quote found
                val_start_in_content = idx + len('"text":') + j
                opening_quote_pos = val_start_in_content

                # Now find the closing quote (before comma or newline)
                # Scan forward, handling escaped quotes
                pos = opening_quote_pos + 1
                value_chars = []
                while pos < len(content):
                    ch = content[pos]
                    if ch == '\\' and pos + 1 < len(content):
                        # Escaped character, keep both
                        value_chars.append(ch)
                        value_chars.append(content[pos+1])
                        pos += 2
                        continue
                    if ch == '"':
                        # Check if this is the closing quote
                        # It should be followed by comma, newline, or end
                        after_val = content[pos+1:pos+10].lstrip()
                        if after_val.startswith(',') or after_val.startswith('\n') or after_val.startswith('\r') or len(after_val.strip()) == 0:
                            # This is the closing quote
                            break
                        else:
                            # This is an embedded quote, escape it
                            value_chars.append('\\')
                            value_chars.append('"')
                            pos += 1
                            continue
                    value_chars.append(ch)
                    pos += 1

                # Reconstruct
                result.append(content[i:val_start_in_content + 1])  # up to and including opening "
                result.append(''.join(value_chars))
                result.append('"')  # closing quote
                i = pos + 1  # skip past closing quote
                continue

        result.append(content[i])
        i += 1

    fixed = ''.join(result)

    try:
        data = json.loads(fixed)
        with open(fpath, 'w', encoding='utf-8') as f:
            f.write(fixed)
        print(f"Fixed {fpath}: {len(data)} items")
    except json.JSONDecodeError as e:
        print(f"Still broken at line {e.lineno}: {e.msg}")
        lines = fixed.split('\n')
        if e.lineno - 1 < len(lines):
            l = lines[e.lineno - 1]
            print(f"  Context: {l[max(0,e.colno-20):e.colno+20]}")
        # Fallback: use regex approach
        fix_via_regex(fpath)

def fix_via_regex(fpath):
    """Alternative fix: read original JSON, apply regex to fix text values"""
    with open(fpath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Replace all unescaped double quotes within text values
    # Pattern: match "text": "..." and fix the value part
    def fix_text_match(m):
        key = m.group(1)  # "text": "
        value = m.group(2)  # the text content
        close = m.group(3)  # ",
        # Escape any remaining double quotes in value
        value = value.replace('"', '\\"')
        return key + value + close

    # Match "text": "CONTENT",
    pattern = r'("text":\s*")(.*?)("(\s*,\s*$|"\s*\n))'
    fixed = re.sub(pattern, fix_text_match, content, flags=re.MULTILINE | re.DOTALL)

    try:
        data = json.loads(fixed)
        with open(fpath, 'w', encoding='utf-8') as f:
            f.write(fixed)
        print(f"Regex fix succeeded: {len(data)} items")
    except json.JSONDecodeError as e:
        print(f"Regex fix also failed: {e}")

fix_json(r'D:\Office-software\project\springboot+vue\docs\论文写作\extracted_ch5_impl_humanized.json')
