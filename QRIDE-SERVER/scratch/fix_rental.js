const fs = require('fs');

let content = fs.readFileSync('Dump20260617.sql', 'utf8');

// Replace row 1
content = content.replace(
    /'1',\s*'1',\s*'1',\s*'2026-05-06T12:11:49',\s*NULL,\s*NULL,\s*NULL,\s*NULL,\s*NULL,\s*'0',\s*'15000',\s*'done',\s*'paid',\s*'2026-05-06T12:11:49'/g,
    "'1', '1', '1', '2026-05-06T12:11:49', NULL, NULL, NULL, NULL, NULL, '0', '15000', 'done', 'paid', '2026-05-06T12:11:49', 0"
);

// Replace row 2
content = content.replace(
    /'2',\s*'1',\s*'1',\s*'2026-05-06T12:31:38',\s*NULL,\s*NULL,\s*NULL,\s*NULL,\s*NULL,\s*'0',\s*'0',\s*'renting',\s*'unpaid',\s*'2026-05-06T12:31:38'/g,
    "'2', '1', '1', '2026-05-06T12:31:38', NULL, NULL, NULL, NULL, NULL, '0', '0', 'renting', 'unpaid', '2026-05-06T12:31:38', 0"
);

fs.writeFileSync('Dump20260617.sql', content, 'utf8');
console.log('Fixed rental rows successfully!');
