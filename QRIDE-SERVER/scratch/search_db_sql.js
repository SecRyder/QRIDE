const fs = require('fs');
const path = 'c:/Users/WIN 11/AndroidStudioProjects/doAn_git/QRIDE/QRIDE-SERVER/db/db_backup1606.sql';

if (fs.existsSync(path)) {
    const content = fs.readFileSync(path, 'utf8');
    const matches = content.match(/CREATE TABLE `?\w+`?/gi);
    console.log("Table definitions found in db_backup1606.sql:");
    console.log(matches ? [...new Set(matches)] : "None");
} else {
    console.log("File not found");
}
