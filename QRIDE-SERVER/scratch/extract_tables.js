const fs = require('fs');
const path = 'c:/Users/WIN 11/AndroidStudioProjects/doAn_git/QRIDE/QRIDE-SERVER/db/db_backup1606.sql';

if (fs.existsSync(path)) {
    const content = fs.readFileSync(path, 'utf8');
    const tables = ['post_comments', 'post_likes', 'reviews', 'trip_images', 'trip_posts'];
    tables.forEach(table => {
        const regex = new RegExp(`CREATE TABLE \`${table}\`[\\s\\S]*?\\);`, 'i');
        const match = content.match(regex);
        if (match) {
            console.log(`--- ${table} ---`);
            console.log(match[0]);
        } else {
            console.log(`--- ${table} not found ---`);
        }
    });
} else {
    console.log("File not found");
}
