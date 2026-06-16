const { execSync } = require('child_process');

try {
    const diff = execSync('git diff main origin/main -- QRIDE-SERVER/server.js', { cwd: 'c:/Users/WIN 11/AndroidStudioProjects/doAn_git/QRIDE', encoding: 'utf8' });
    const lines = diff.split('\n');
    const matched = [];
    for (let i = 0; i < lines.length; i++) {
        if (lines[i].includes('vehicles') || lines[i].includes('/vehicles/:')) {
            matched.push(lines.slice(Math.max(0, i - 10), Math.min(lines.length, i + 10)).join('\n'));
            i += 10; // skip ahead to avoid overlapping matches
        }
    }
    console.log(`Matched count: ${matched.length}`);
    matched.forEach((m, idx) => {
        console.log(`--- Match ${idx + 1} ---`);
        console.log(m);
    });
} catch (e) {
    console.error(e.message);
}
