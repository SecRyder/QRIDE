const http = require('http');

http.get('http://localhost:3000/api/vehicles/1', (res) => {
    let data = '';
    res.on('data', (chunk) => { data += chunk; });
    res.on('end', () => {
        try {
            console.log("Status Code:", res.statusCode);
            console.log("Response:", JSON.stringify(JSON.parse(data), null, 2));
        } catch (e) {
            console.log("Error parsing response:", e.message);
            console.log("Raw Response:", data);
        }
    });
}).on('error', (err) => {
    console.error("API Request Error:", err.message);
});
