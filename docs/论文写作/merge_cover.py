"""
String-level XML merge: cover.docx cover pages + thesis.docx body.
- Extract cover body children (all except final sectPr)
- Split thesis body at first section break (end of user's copy-pasted cover section)
- Combine and pack
"""
import os, re, shutil, subprocess

COVER_DIR = r'C:\Users\lizhaoliang\Desktop\cover_unpacked'
THESIS_DIR = r'C:\Users\lizhaoliang\Desktop\thesis_unpacked'
OUT_DIR = r'C:\Users\lizhaoliang\Desktop\merged_unpacked'
OUT_DOCX = r'D:\Office-software\project\springboot+vue\计科2202李昭良-基于springboot+vue校园报修管理系统设计与实现.docx'

# ============================================================
# 1. Read source XMLs
# ============================================================
with open(os.path.join(COVER_DIR, 'word', 'document.xml'), 'r', encoding='utf-8') as f:
    cover_xml = f.read()
with open(os.path.join(THESIS_DIR, 'word', 'document.xml'), 'r', encoding='utf-8') as f:
    thesis_xml = f.read()

# ============================================================
# 2. Extract cover body content (without final sectPr or </w:body>)
# ============================================================
cover_body_start = cover_xml.index('<w:body')
cover_body_tag_end = cover_xml.index('>', cover_body_start) + 1
cover_body_tag = cover_xml[cover_body_start:cover_body_tag_end]
cover_body_close = cover_xml.index('</w:body>')

# Get everything between <w:body...> and </w:body>
cover_body_inner = cover_xml[cover_body_tag_end:cover_body_close]

# Remove the final sectPr from cover (last <w:sectPr...> before potential whitespace)
# Find last <w:sectPr
last_sectpr = cover_body_inner.rfind('<w:sectPr')
if last_sectpr >= 0:
    cover_body_inner = cover_body_inner[:last_sectpr].rstrip()

print(f'Cover body inner: {len(cover_body_inner)} chars')

# ============================================================
# 3. Extract thesis body and split at first section break
# ============================================================
thesis_body_start = thesis_xml.index('<w:body')
thesis_body_tag_end = thesis_xml.index('>', thesis_body_start) + 1
thesis_body_close = thesis_xml.index('</w:body>')

# Everything before body, the body tag, and everything after </w:body>
thesis_before_body = thesis_xml[:thesis_body_start]
thesis_after_body = thesis_xml[thesis_body_close + len('</w:body>'):]

thesis_body_inner = thesis_xml[thesis_body_tag_end:thesis_body_close]

# Find first section break PARAGRAPH
# We need the <w:p that contains <w:pPr><w:sectPr> (this paragraph ends section 0)
# We keep this paragraph as the section separator between cover and thesis body
break_p_start = None
break_p_end = None
for m in re.finditer(r'</w:sectPr>', thesis_body_inner):
    after_sect = thesis_body_inner[m.end():m.end()+300]
    pp_close = after_sect.find('</w:pPr>')
    if 0 <= pp_close < 50:
        rest = after_sect[pp_close+7:]
        p_close = rest.find('</w:p>')
        if 0 <= p_close < 50:
            # Found the section break position
            end_of_break = m.end() + pp_close + 7 + p_close + 6
            # Search backwards for the <w:p> tag that starts this section break paragraph
            # Must match exactly <w:p followed by space, >, or newline — NOT <w:pPr, <w:pgMar etc.
            # We want the last <w:p tag before end_of_break whose closing </w:p> is at or after end_of_break
            for p_match in re.finditer(r'<w:p[\s>]', thesis_body_inner):
                p_start = p_match.start()
                if p_start >= end_of_break:
                    break
                p_end = thesis_body_inner.find('</w:p>', p_start)
                if p_end is not None and p_end >= end_of_break - 10:
                    break_p_start = p_start  # keep updating to last valid match
                    break_p_end = end_of_break

            assert break_p_start is not None
            print(f'Section break paragraph: [{break_p_start}:{break_p_end}] ({break_p_end-break_p_start} chars)')

            # Split: everything BEFORE the break paragraph (thesis section 0)
            # Then keep the break paragraph as separator
            # Then everything AFTER the break paragraph (thesis section 1+)
            thesis_before_break = thesis_body_inner[:break_p_start]
            thesis_break_para = thesis_body_inner[break_p_start:break_p_end]
            thesis_after_break = thesis_body_inner[break_p_end:]

            print(f'Before break: {len(thesis_before_break)} chars')
            print(f'Break paragraph: {len(thesis_break_para)} chars')
            print(f'After break: {len(thesis_after_break)} chars')
            print(f'After break starts: {thesis_after_break[:100]}')
            break

if break_p_start is None:
    print('ERROR: Could not find first section break!')
    exit(1)

# Build thesis section 1+ content (including the break paragraph as separator)
thesis_s1_onwards = thesis_break_para + '\n' + thesis_after_break.lstrip()

# The thesis_s1_onwards should contain all remaining sections (1-13)
# plus the final </w:body>
print(f'Thesis from S1: {len(thesis_s1_onwards)} chars')

# ============================================================
# 4. Build merged document.xml
# ============================================================
# Structure:
#   <?xml...><w:document ...namespaces...>
#     <w:body>
#       cover_body_inner    (cover doc's sections 0-1 without final sectPr)
#       thesis_s1_onwards   (thesis sections 1-13 plus final sectPr + </w:body>)
#     </w:document>

merged_xml = (
    thesis_before_body +
    cover_body_tag +
    '\n' + cover_body_inner + '\n' +
    thesis_s1_onwards + '\n' +
    '</w:body>\n' +
    thesis_after_body
)

# Quick validation: check that it has proper XML structure
if not merged_xml.strip().startswith('<?xml'):
    print('WARNING: merged XML does not start with <?xml')
if '<w:body' not in merged_xml:
    print('ERROR: no body tag in merged XML')
    exit(1)
if '</w:body>' not in merged_xml:
    print('ERROR: no body close tag')
    exit(1)
if '</w:document>' not in merged_xml:
    print('ERROR: no document close tag')
    exit(1)

print(f'Merged XML: {len(merged_xml)} chars')

# ============================================================
# 5. Write output
# ============================================================
if os.path.exists(OUT_DIR):
    shutil.rmtree(OUT_DIR)
shutil.copytree(THESIS_DIR, OUT_DIR)

out_doc = os.path.join(OUT_DIR, 'word', 'document.xml')
with open(out_doc, 'w', encoding='utf-8') as f:
    f.write(merged_xml)

# ============================================================
# 6. Copy cover media
# ============================================================
cover_media = os.path.join(COVER_DIR, 'word', 'media')
thesis_media = os.path.join(OUT_DIR, 'word', 'media')

if os.path.exists(cover_media) and os.path.isdir(cover_media):
    if not os.path.exists(thesis_media):
        os.makedirs(thesis_media)
    existing = set(os.listdir(thesis_media))
    max_n = 0
    for f in existing:
        m = re.match(r'image(\d+)', f)
        if m:
            max_n = max(max_n, int(m.group(1)))

    for img in sorted(os.listdir(cover_media)):
        ext = img.rsplit('.', 1)[-1] if '.' in img else 'png'
        max_n += 1
        new_name = f'image{max_n}.{ext}'
        src = os.path.join(cover_media, img)
        dst = os.path.join(thesis_media, new_name)
        shutil.copy2(src, dst)
        print(f'Media: {img} -> {new_name}')

# ============================================================
# 7. Merge [Content_Types].xml
# ============================================================
ct_cover = os.path.join(COVER_DIR, '[Content_Types].xml')
ct_thesis = os.path.join(OUT_DIR, '[Content_Types].xml')
if os.path.exists(ct_cover) and os.path.exists(ct_thesis):
    with open(ct_cover, 'r', encoding='utf-8') as f:
        ct_c = f.read()
    with open(ct_thesis, 'r', encoding='utf-8') as f:
        ct_t = f.read()
    for ext in ['png', 'jpeg', 'jpg', 'gif']:
        if f'Extension="{ext}"' in ct_c and f'Extension="{ext}"' not in ct_t:
            mime = f'image/{ext}' if ext != 'jpg' else 'image/jpeg'
            ins = ct_t.rfind('</Types>')
            ct_t = ct_t[:ins] + f'  <Default Extension="{ext}" ContentType="{mime}"/>\n' + ct_t[ins:]
    with open(ct_thesis, 'w', encoding='utf-8') as f:
        f.write(ct_t)
    print('Content types merged')

# ============================================================
# 8. Merge relationships
# ============================================================
cr = os.path.join(COVER_DIR, 'word', '_rels', 'document.xml.rels')
tr = os.path.join(OUT_DIR, 'word', '_rels', 'document.xml.rels')
if os.path.exists(cr) and os.path.exists(tr):
    with open(cr, 'r', encoding='utf-8') as f:
        cr_xml = f.read()
    with open(tr, 'r', encoding='utf-8') as f:
        tr_xml = f.read()

    # Extract image relationships from cover
    img_rels = re.findall(r'<Relationship[^>]*Type="[^"]*image[^"]*"[^>]*/>', cr_xml)
    if img_rels:
        ins = tr_xml.rfind('</Relationships>')
        for rel in img_rels:
            tr_xml = tr_xml[:ins] + '  ' + rel + '\n' + tr_xml[ins:]
        with open(tr, 'w', encoding='utf-8') as f:
            f.write(tr_xml)
        print(f'Merged {len(img_rels)} image rels')

# ============================================================
# 9. Pack with Python zipfile (avoids pack.py path encoding issues)
# ============================================================
print('\nPacking...')
import zipfile as zf

out_docx_path = r'D:\Office-software\project\springboot+vue\thesis_merged.docx'
if os.path.exists(out_docx_path):
    os.remove(out_docx_path)

with zf.ZipFile(out_docx_path, 'w', zf.ZIP_DEFLATED) as zout:
    for root, dirs, files in os.walk(OUT_DIR):
        for file in files:
            file_path = os.path.join(root, file)
            arcname = os.path.relpath(file_path, OUT_DIR).replace(os.sep, '/')
            zout.write(file_path, arcname)

print(f'Created: {out_docx_path}')
print(f'Size: {os.path.getsize(out_docx_path) / 1024:.1f} KB')
print('Done!')
