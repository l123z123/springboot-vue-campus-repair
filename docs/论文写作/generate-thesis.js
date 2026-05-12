const fs = require('fs');
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, HeadingLevel, BorderStyle, WidthType,
  ShadingType, PageBreak, PageNumber, TableOfContents,
  LevelFormat
} = require('docx');

const BASE = 'D:/Office-software/project/springboot+vue/docs/论文写作';

// ============================================================
// Global reference list (27 items, from Chapter 7)
// ============================================================
const GLOBAL_REFS = [
  null, // index 0 unused
  '山锋, 丑洋. 基于互联网新技术的高校后勤报修系统应用研究[J]. 数字技术与应用, 2020, 38(1):56-57.',
  '肖智兵, 王亮, 王小红. 基于微信小程序的高校后勤报修系统——以湖北职业技术学院为例的设计与实现[J]. 现代信息科技, 2020, 4(23).',
  '刘锦. 智慧校园故障报修信息管理系统开发研究[J]. 计算机与网络, 2024.',
  '王福强. Spring Boot实战[M]. 北京: 人民邮电出版社, 2019.',
  '梁灏. Vue.js实战[M]. 北京: 清华大学出版社, 2018.',
  '李刚. 疯狂Java讲义（第5版）[M]. 北京: 电子工业出版社, 2019.',
  '黄勇. 架构探险:从零开始写Java Web框架[M]. 北京: 电子工业出版社, 2015.',
  '郝佳. Spring源码深度解析（第2版）[M]. 北京: 人民邮电出版社, 2019.',
  '周立. Spring Cloud与Docker微服务架构实战（第2版）[M]. 北京: 电子工业出版社, 2018.',
  '杨开振. 深入浅出MyBatis技术原理与实战[M]. 北京: 电子工业出版社, 2016.',
  '明日科技. Java从入门到精通（第5版）[M]. 北京: 清华大学出版社, 2019.',
  '刘增辉. MyBatis从入门到精通[M]. 北京: 电子工业出版社, 2017.',
  '张耀春. Vue.js权威指南[M]. 北京: 电子工业出版社, 2016.',
  '王红元. 微信小程序开发实战[M]. 北京: 电子工业出版社, 2018.',
  '李兴华. Java Web开发实战经典[M]. 北京: 清华大学出版社, 2014.',
  '黄文毅. Spring 5企业级开发实战[M]. 北京: 清华大学出版社, 2019.',
  '徐丽. MySQL 8.0从入门到精通[M]. 北京: 电子工业出版社, 2019.',
  '孙卫琴. Tomcat与Java Web开发技术详解（第3版）[M]. 北京: 电子工业出版社, 2019.',
  '李子晔. Redis设计与实现[M]. 北京: 机械工业出版社, 2014.',
  '李智慧. 大型网站技术架构:核心原理与案例分析[M]. 北京: 电子工业出版社, 2013.',
  '刘博. Spring Boot企业级应用开发实战[M]. 北京: 电子工业出版社, 2018.',
  '许令波. 深入分析Java Web技术内幕（修订版）[M]. 北京: 电子工业出版社, 2014.',
  '张海藩. 软件工程导论（第6版）[M]. 北京: 清华大学出版社, 2013.',
  '张书豪. 高校后勤信息化建设研究[J]. 中国教育信息化, 2019(5): 45-48.',
  'Raymond M. Facility Management Systems in Higher Education: A Comprehensive Review[J]. Journal of Educational Technology, 2018, 15(3): 234-248.',
  'Ken S. A Study of Facility Maintenance Systems Based on IoT Technology[J]. International Journal of Smart Campus, 2021, 8(2): 112-125.',
  'Tanaka H. Standardization of University Facility Management in Japan[J]. Journal of Higher Education Management, 2019, 22(4): 301-315.',
];

// ============================================================
// Chapter-based reference mappings (chapter local -> global)
// Key references:
//   山锋→1, 肖智兵→2, 刘锦→3, Raymond→25, Ken→26, Tanaka→27
//   SpringBoot→4/21, Vue→5, MyBatis→10/12, MySQL→17,
//   Redis→19, Security→8/16, JWT/Web generic→others
// ============================================================
const CH1_MAP = { 1:1, 2:1, 3:3, 4:24, 5:24, 6:25, 7:26, 8:27, 9:3, 10:1, 11:2, 12:21, 13:5, 14:23 };
const CH2_MAP = { 1:6, 2:17, 3:10, 4:19, 5:19, 6:19, 7:4, 8:4, 9:10, 10:10, 11:6, 12:6, 13:21, 14:5, 15:5, 16:5, 17:13, 18:5 };
const CH3_MAP = { 1:4, 2:5, 3:24, 4:23, 5:2, 6:1, 7:3, 8:19 };
const CH4_MAP = { 1:4, 2:23, 3:17, 4:21, 5:6 };
const CH5_MAP = { 1:4, 2:19, 3:21, 4:3, 5:1, 6:18, 7:19, 8:5 };
const CH6_MAP = { 1:17, 2:13, 3:23 };

// ============================================================
// Helper: remap references in text
// ============================================================
function remapRefs(text, map) {
  return text.replace(/\[(\d+)\]/g, (match, num) => {
    const globalNum = map[parseInt(num)];
    return globalNum ? `[${globalNum}]` : match;
  });
}

// ============================================================
// Read a chapter file with reference remapping
// ============================================================
function readChapter(filename, refMap) {
  let text = fs.readFileSync(`${BASE}/${filename}`, 'utf-8');
  if (refMap) {
    text = remapRefs(text, refMap);
  }
  return text;
}

// ============================================================
// Font constants
// ============================================================
const FONT_BODY = '宋体';
const FONT_HEADING = '黑体';
const FONT_EN = 'Times New Roman';
const SIZE_BODY = 24; // 12pt = 小四
const SIZE_H1 = 32;   // 16pt = 三号 (chapter titles)
const SIZE_H2 = 28;   // 14pt = 四号 (section titles)
const SIZE_H3 = 26;   // 13pt = 小四加粗 (subsection)
const SIZE_TITLE = 44; // 22pt = 二号 (cover title)
const SIZE_COVER = 28; // 14pt (cover info)

// ============================================================
// Element builders
// ============================================================
function bodyPara(text, opts = {}) {
  const runs = parseInlineFormat(text);
  return new Paragraph({
    spacing: { line: 360, before: 0, after: 0 },
    indent: opts.indent !== false ? { firstLine: 480 } : undefined,
    alignment: opts.align || AlignmentType.JUSTIFIED,
    children: runs,
  });
}

function emptyPara() {
  return new Paragraph({ spacing: { line: 360 }, children: [] });
}

function chapterTitle(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    spacing: { before: 360, after: 240, line: 360 },
    alignment: AlignmentType.CENTER,
    children: [new TextRun({ text, font: FONT_HEADING, size: SIZE_H1, bold: true })],
  });
}

function sectionTitle(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    spacing: { before: 240, after: 180, line: 360 },
    children: [new TextRun({ text, font: FONT_HEADING, size: SIZE_H2, bold: true })],
  });
}

function subSectionTitle(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_3,
    spacing: { before: 200, after: 120, line: 360 },
    children: [new TextRun({ text, font: FONT_HEADING, size: SIZE_H3, bold: true })],
  });
}

function centeredPara(text, opts = {}) {
  return new Paragraph({
    spacing: { line: 360 },
    alignment: AlignmentType.CENTER,
    children: [new TextRun({ text, font: opts.font || FONT_BODY, size: opts.size || SIZE_BODY, bold: opts.bold || false })],
  });
}

function parseInlineFormat(text) {
  if (!text) return [new TextRun('')];
  const runs = [];
  let remaining = text;
  while (remaining.length > 0) {
    const boldMatch = remaining.match(/^(.*?)\*\*(.+?)\*\*/);
    if (boldMatch) {
      if (boldMatch[1]) {
        runs.push(new TextRun({ text: boldMatch[1], font: FONT_BODY, size: SIZE_BODY }));
      }
      runs.push(new TextRun({ text: boldMatch[2], font: FONT_BODY, size: SIZE_BODY, bold: true }));
      remaining = remaining.slice(boldMatch[0].length);
    } else {
      runs.push(new TextRun({ text: remaining, font: FONT_BODY, size: SIZE_BODY }));
      remaining = '';
    }
  }
  if (runs.length === 0) runs.push(new TextRun({ text, font: FONT_BODY, size: SIZE_BODY }));
  return runs;
}

// Parse markdown table lines into docx Table
const tableBorder = { style: BorderStyle.SINGLE, size: 1, color: '000000' };
const tableBorders = { top: tableBorder, bottom: tableBorder, left: tableBorder, right: tableBorder };
const tableBorderNone = { style: BorderStyle.NONE, size: 0 };
const tableBordersNone = { top: tableBorderNone, bottom: tableBorderNone, left: tableBorderNone, right: tableBorderNone };

function parseTable(lines) {
  const dataRows = [];
  for (const line of lines) {
    if (line.match(/^\|[-:\s|]+\|$/)) continue; // separator row
    const cells = line.split('|').filter(c => c.trim() !== '').map(c => c.trim());
    dataRows.push(cells);
  }
  if (dataRows.length === 0) return null;

  const colCount = Math.max(...dataRows.map(r => r.length));
  const colWidth = Math.floor(9026 / colCount); // A4 content width / columns

  const rows = dataRows.map((rowData, rowIdx) => {
    const isHeader = rowIdx === 0;
    const cells = rowData.map(cellText => {
      return new TableCell({
        borders: tableBorders,
        width: { size: colWidth, type: WidthType.DXA },
        shading: isHeader ? { fill: 'D9E2F3', type: ShadingType.CLEAR } : undefined,
        margins: { top: 60, bottom: 60, left: 100, right: 100 },
        verticalAlign: 'center',
        children: [new Paragraph({
          spacing: { line: 300 },
          alignment: AlignmentType.CENTER,
          children: [new TextRun({ text: cellText, font: FONT_BODY, size: 20, bold: isHeader })],
        })],
      });
    });
    // Pad to colCount
    while (cells.length < colCount) {
      cells.push(new TableCell({
        borders: tableBorders,
        width: { size: colWidth, type: WidthType.DXA },
        margins: { top: 60, bottom: 60, left: 100, right: 100 },
        children: [new Paragraph({ spacing: { line: 300 }, children: [] })],
      }));
    }
    return new TableRow({ children: cells });
  });

  return new Table({
    width: { size: 9026, type: WidthType.DXA },
    columnWidths: Array(colCount).fill(colWidth),
    rows,
  });
}

// ============================================================
// Convert markdown text to docx children array
// ============================================================
function mdToDocx(mdText, fileChapterNum) {
  const children = [];
  const lines = mdText.split('\n');
  let i = 0;
  let tableLines = [];
  let inTable = false;

  while (i < lines.length) {
    const line = lines[i];

    // Handle tables
    if (line.startsWith('|') && line.endsWith('|')) {
      inTable = true;
      tableLines.push(line);
      i++;
      continue;
    } else if (inTable) {
      // Flush table
      const table = parseTable(tableLines);
      if (table) {
        children.push(emptyPara());
        children.push(table);
        children.push(emptyPara());
      }
      tableLines = [];
      inTable = false;
    }

    // Skip horizontal rules and empty markdown
    if (line.trim() === '---' || line.trim() === '') {
      i++;
      continue;
    }

    // Chapter title (# 第X章 ...)
    if (line.match(/^# 第\d+章/)) {
      const title = line.replace(/^# /, '');
      children.push(chapterTitle(title));
      i++;
      continue;
    }

    // Section title (## X.X ...)
    if (line.match(/^## \d+\.\d+/)) {
      const title = line.replace(/^## /, '');
      children.push(sectionTitle(title));
      i++;
      continue;
    }

    // Subsection (### X.X.X ...)
    if (line.match(/^### \d+\.\d+\.\d+/)) {
      const title = line.replace(/^### /, '');
      children.push(subSectionTitle(title));
      i++;
      continue;
    }

    // Special handling for 摘要/Abstract/参考文献/致谢 headings
    if (line.match(/^# (摘要|Abstract|参考文献|致谢)/)) {
      const title = line.replace(/^# /, '');
      children.push(chapterTitle(title));
      i++;
      continue;
    }

    // # 第X章 without detail (just chapter number)
    if (line.match(/^# 第\d+章$/)) {
      children.push(chapterTitle(line.replace(/^# /, '')));
      i++;
      continue;
    }

    // Regular headings
    if (line.match(/^#{1,3} /)) {
      const level = line.match(/^(#{1,3}) /)[1].length;
      const title = line.replace(/^#{1,3} /, '');
      if (level === 1) children.push(chapterTitle(title));
      else if (level === 2) children.push(sectionTitle(title));
      else children.push(subSectionTitle(title));
      i++;
      continue;
    }

    // **Bold titles** (like **表6.1 ...**)
    if (line.match(/^\*\*表\d+\.\d+/)) {
      const text = line.replace(/\*\*/g, '');
      children.push(new Paragraph({
        spacing: { before: 120, after: 60, line: 360 },
        alignment: AlignmentType.CENTER,
        children: [new TextRun({ text, font: FONT_HEADING, size: 20, bold: true })],
      }));
      i++;
      continue;
    }

    // 用例编号 etc bold labels
    if (line.match(/^\*\*.+\*\*/) && !line.startsWith('|')) {
      children.push(bodyPara(line));
      i++;
      continue;
    }

    // Regular paragraph
    children.push(bodyPara(line));
    i++;
  }

  // Flush any remaining table
  if (inTable && tableLines.length > 0) {
    const table = parseTable(tableLines);
    if (table) {
      children.push(emptyPara());
      children.push(table);
      children.push(emptyPara());
    }
  }

  return children;
}

// ============================================================
// Load all chapters with reference remapping
// ============================================================
const ch1 = readChapter('第1章-绪论-草稿.md', CH1_MAP);
const ch2 = readChapter('第2章-技术介绍-草稿.md', CH2_MAP);
const ch3 = readChapter('第3章-需求分析-草稿.md', CH3_MAP);
const ch4 = readChapter('第4章-系统设计-草稿.md', CH4_MAP);
const ch5 = readChapter('第5章-系统实现-草稿.md', CH5_MAP);
const ch6 = readChapter('第6章-系统测试-草稿.md', CH6_MAP);

// Chapter 7 contains the reference list and acknowledgements - load separately
const ch7Raw = fs.readFileSync(`${BASE}/第7章-总结与展望-草稿.md`, 'utf-8');

// Split Chapter 7 into: summary, references, acknowledgements
const refIdx = ch7Raw.indexOf('# 参考文献');
const ackIdx = ch7Raw.indexOf('# 致谢');

const ch7Summary = ch7Raw.slice(0, refIdx).trim();
const ch7Refs = ch7Raw.slice(refIdx, ackIdx).trim();
const ch7Ack = ch7Raw.slice(ackIdx).trim();

// Load abstract
const abstractRaw = fs.readFileSync(`${BASE}/摘要-草稿.md`, 'utf-8');
const absSplit = abstractRaw.indexOf('# Abstract');
const cnAbstract = abstractRaw.slice(0, absSplit).trim();
const enAbstract = abstractRaw.slice(absSplit).trim();

// ============================================================
// Build document sections
// ============================================================

// --- Cover page ---
const coverChildren = [
  emptyPara(), emptyPara(), emptyPara(), emptyPara(),
  emptyPara(), emptyPara(), emptyPara(), emptyPara(),
  centeredPara('本科毕业论文', { font: FONT_HEADING, size: SIZE_TITLE, bold: true }),
  emptyPara(), emptyPara(),
  centeredPara('基于SpringBoot+Vue的校园报修管理系统的设计与实现', { font: FONT_HEADING, size: 36, bold: true }),
  emptyPara(), emptyPara(), emptyPara(), emptyPara(),
  centeredPara('学    院：计算机学院', { font: FONT_BODY, size: SIZE_COVER }),
  centeredPara('专    业：计算机科学与技术', { font: FONT_BODY, size: SIZE_COVER }),
  centeredPara('学    号：计科2202', { font: FONT_BODY, size: SIZE_COVER }),
  centeredPara('姓    名：李昭良', { font: FONT_BODY, size: SIZE_COVER }),
  centeredPara('指导教师：袁海军', { font: FONT_BODY, size: SIZE_COVER }),
  emptyPara(), emptyPara(),
  centeredPara('2026年5月', { font: FONT_BODY, size: SIZE_COVER }),
];

// --- Chinese Abstract ---
const abstractChildren = mdToDocx(cnAbstract);
// Prepend "摘要" heading removed by mdToDocx since it matches # pattern

// --- English Abstract ---
const enAbstractText = enAbstract.replace('# Abstract', '');
const enAbstractChildren = [
  chapterTitle('Abstract'),
  ...mdToDocx(enAbstractText),
];

// --- TOC ---
const tocChildren = [
  chapterTitle('目  录'),
  new TableOfContents('目录', { hyperlink: true, headingStyleRange: '1-3' }),
];

// --- Chapters ---
const ch1Children = mdToDocx(ch1);
const ch2Children = mdToDocx(ch2);
const ch3Children = mdToDocx(ch3);
const ch4Children = mdToDocx(ch4);
const ch5Children = mdToDocx(ch5);
const ch6Children = mdToDocx(ch6);
const ch7Children = mdToDocx(ch7Summary);

// --- References ---
const refChildren = [
  chapterTitle('参考文献'),
];
for (let i = 1; i <= 27; i++) {
  refChildren.push(new Paragraph({
    spacing: { line: 360 },
    indent: { left: 480, hanging: 480 },
    children: [new TextRun({ text: `[${i}] ${GLOBAL_REFS[i]}`, font: FONT_BODY, size: 21 })],
  }));
}

// --- Acknowledgements ---
const ackText = ch7Ack.replace('# 致谢\n\n', '');
const ackChildren = [
  chapterTitle('致  谢'),
  ...ackText.split('\n\n').filter(p => p.trim()).map(p =>
    new Paragraph({
      spacing: { line: 360 },
      indent: { firstLine: 480 },
      children: [new TextRun({ text: p.trim(), font: FONT_BODY, size: SIZE_BODY })],
    })
  ),
  emptyPara(),
];

// ============================================================
// Create Document
// ============================================================
const doc = new Document({
  styles: {
    default: {
      document: {
        run: { font: FONT_BODY, size: SIZE_BODY },
      },
    },
    paragraphStyles: [
      {
        id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { size: SIZE_H1, bold: true, font: FONT_HEADING },
        paragraph: { spacing: { before: 360, after: 240 }, outlineLevel: 0 },
      },
      {
        id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { size: SIZE_H2, bold: true, font: FONT_HEADING },
        paragraph: { spacing: { before: 240, after: 180 }, outlineLevel: 1 },
      },
      {
        id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { size: SIZE_H3, bold: true, font: FONT_HEADING },
        paragraph: { spacing: { before: 200, after: 120 }, outlineLevel: 2 },
      },
    ],
  },
  sections: [
    // Cover page - no header/footer
    {
      properties: {
        page: {
          size: { width: 11906, height: 16838 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      children: coverChildren,
    },
    // Abstract - Roman page numbers
    {
      properties: {
        page: {
          size: { width: 11906, height: 16838 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ text: '基于SpringBoot+Vue的校园报修管理系统的设计与实现', font: FONT_BODY, size: 18 })],
            border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: '000000', space: 4 } },
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT_EN, size: 20 })],
          })],
        }),
      },
      children: abstractChildren,
    },
    // English Abstract
    {
      properties: {
        page: {
          size: { width: 11906, height: 16838 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ text: '基于SpringBoot+Vue的校园报修管理系统的设计与实现', font: FONT_BODY, size: 18 })],
            border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: '000000', space: 4 } },
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT_EN, size: 20 })],
          })],
        }),
      },
      children: enAbstractChildren,
    },
    // TOC
    {
      properties: {
        page: {
          size: { width: 11906, height: 16838 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ text: '基于SpringBoot+Vue的校园报修管理系统的设计与实现', font: FONT_BODY, size: 18 })],
            border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: '000000', space: 4 } },
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT_EN, size: 20 })],
          })],
        }),
      },
      children: tocChildren,
    },
    // Chapter 1
    {
      properties: {
        page: {
          size: { width: 11906, height: 16838 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ text: '基于SpringBoot+Vue的校园报修管理系统的设计与实现', font: FONT_BODY, size: 18 })],
            border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: '000000', space: 4 } },
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT_EN, size: 20 })],
          })],
        }),
      },
      children: ch1Children,
    },
    // Chapter 2
    {
      properties: {
        page: {
          size: { width: 11906, height: 16838 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ text: '基于SpringBoot+Vue的校园报修管理系统的设计与实现', font: FONT_BODY, size: 18 })],
            border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: '000000', space: 4 } },
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT_EN, size: 20 })],
          })],
        }),
      },
      children: ch2Children,
    },
    // Chapter 3
    {
      properties: {
        page: {
          size: { width: 11906, height: 16838 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ text: '基于SpringBoot+Vue的校园报修管理系统的设计与实现', font: FONT_BODY, size: 18 })],
            border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: '000000', space: 4 } },
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT_EN, size: 20 })],
          })],
        }),
      },
      children: ch3Children,
    },
    // Chapter 4
    {
      properties: {
        page: {
          size: { width: 11906, height: 16838 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ text: '基于SpringBoot+Vue的校园报修管理系统的设计与实现', font: FONT_BODY, size: 18 })],
            border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: '000000', space: 4 } },
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT_EN, size: 20 })],
          })],
        }),
      },
      children: ch4Children,
    },
    // Chapter 5
    {
      properties: {
        page: {
          size: { width: 11906, height: 16838 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ text: '基于SpringBoot+Vue的校园报修管理系统的设计与实现', font: FONT_BODY, size: 18 })],
            border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: '000000', space: 4 } },
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT_EN, size: 20 })],
          })],
        }),
      },
      children: ch5Children,
    },
    // Chapter 6
    {
      properties: {
        page: {
          size: { width: 11906, height: 16838 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ text: '基于SpringBoot+Vue的校园报修管理系统的设计与实现', font: FONT_BODY, size: 18 })],
            border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: '000000', space: 4 } },
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT_EN, size: 20 })],
          })],
        }),
      },
      children: ch6Children,
    },
    // Chapter 7
    {
      properties: {
        page: {
          size: { width: 11906, height: 16838 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ text: '基于SpringBoot+Vue的校园报修管理系统的设计与实现', font: FONT_BODY, size: 18 })],
            border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: '000000', space: 4 } },
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT_EN, size: 20 })],
          })],
        }),
      },
      children: ch7Children,
    },
    // References
    {
      properties: {
        page: {
          size: { width: 11906, height: 16838 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ text: '基于SpringBoot+Vue的校园报修管理系统的设计与实现', font: FONT_BODY, size: 18 })],
            border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: '000000', space: 4 } },
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT_EN, size: 20 })],
          })],
        }),
      },
      children: refChildren,
    },
    // Acknowledgements
    {
      properties: {
        page: {
          size: { width: 11906, height: 16838 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ text: '基于SpringBoot+Vue的校园报修管理系统的设计与实现', font: FONT_BODY, size: 18 })],
            border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: '000000', space: 4 } },
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT_EN, size: 20 })],
          })],
        }),
      },
      children: ackChildren,
    },
  ],
});

// ============================================================
// Output
// ============================================================
const OUT = 'D:/Office-software/project/springboot+vue/计科2202李昭良-基于springboot+vue校园报修管理系统设计与实现.docx';
Packer.toBuffer(doc).then(buffer => {
  fs.writeFileSync(OUT, buffer);
  console.log(`Done: ${OUT}`);
  console.log(`Size: ${(buffer.length / 1024).toFixed(1)} KB`);
}).catch(err => {
  console.error('Error:', err.message);
  process.exit(1);
});
