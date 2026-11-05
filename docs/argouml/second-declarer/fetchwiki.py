import io, re, os, sys, html, urllib.request, urllib.parse

BASE = 'https://argouml-tigris-org.github.io/tigris/wiki-argouml/wiki/'
OUT = sys.argv[1] if len(sys.argv) > 1 else 'wiki'
os.makedirs(OUT, exist_ok=True)
pages = [
    'ArgoUML_Design,_The_Big_Picture', 'Definition_of_subsystem', 'Relationship_of_the_subsystems', 'External_subsystems',
    'When_Considering_Dependencies',
    '<<Layer>>_Low-level_subsystems', '<<Layer>>_View_and_Control_subsystems', '<<Layer>>_Loadable_subsystems',
    '<<Layer>>_Top_Level_subsystem',
    '<<Subsystem>>_Application', '<<Subsystem>>_Code_Generation_Subsystem', '<<Subsystem>>_Configuration',
    '<<Subsystem>>_Critics_and_other_cognitive_tools', '<<Subsystem>>_Diagrams', '<<Subsystem>>_Explorer',
    '<<Subsystem>>_Help_System', '<<Subsystem>>_Internationalization',
    '<<Subsystem>>_Java_-_Code_Generation_and_Reverse_Engineering', '<<Subsystem>>_Logging', '<<Subsystem>>_Model',
    '<<Subsystem>>_Module_loader', '<<Subsystem>>_Notation', '<<Subsystem>>_OCL',
    '<<Subsystem>>_Other_source_languages_(C++,_Python,_etc)', '<<Subsystem>>_Persistence', '<<Subsystem>>_Profile',
    '<<Subsystem>>_Property_panels', '<<Subsystem>>_Reverse_Engineering_Subsystem', '<<Subsystem>>_TaskManagement',
    '<<Subsystem>>_The_GUI', '<<Subsystem>>_To_Do_items',
]
for i, p in enumerate(pages, 1):
    url = BASE + urllib.parse.quote(p + '.html')
    try:
        h = urllib.request.urlopen(url, timeout=60).read().decode('utf-8', 'replace')
    except Exception as e:
        print('FAIL', p, e)
        continue
    m = re.search(r'<section[^>]*>(.*)</section>', h, re.S) or re.search(r'<body[^>]*>(.*)</body>', h, re.S)
    t = m.group(1) if m else h
    t = re.sub(r'<script.*?</script>|<style.*?</style>', '', t, flags=re.S)
    t = re.sub(r'</(p|div|li|h\d|tr|br|dd|dt)>', '\n', t)
    t = re.sub(r'<(br|li)[^>]*>', '\n', t)
    t = re.sub(r'<[^>]+>', '', t)
    t = html.unescape(t)
    t = re.sub(r'[ \t]+', ' ', t)
    t = re.sub(r'\n\s*\n+', '\n\n', t).strip()
    name = '%02d-%s.txt' % (i, re.sub(r'[^A-Za-z0-9_-]+', '_', p))
    io.open(os.path.join(OUT, name), 'w', encoding='utf-8', newline='\n').write('Source: ' + url + '\n\n' + t + '\n')
    print(name, len(t))
