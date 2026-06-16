const { execSync } = require('child_process');

try {
    const diff = execSync('git diff origin/main -- QRIDE-SERVER/server.js', { cwd: 'c:/Users/WIN 11/AndroidStudioProjects/doAn_git/QRIDE', encoding: 'utf8' });
    if (!diff) {
        console.log("No differences between local working copy and origin/main for server.js!");
    } else {
        console.log("Differences found:");
        console.log(diff);
    }
} catch (e) {
    console.error(e.message);
}
