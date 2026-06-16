const express = require('express');
const axios = require('axios'); // npm install axios nếu chưa có
const router = express.Router();

const DIRECTIONS_API_KEY = process.env.DIRECTIONS_API_KEY; // Lấy từ .env

// Helper to calculate distance between two coordinates in meters
function getDistance(lat1, lon1, lat2, lon2) {
    const R = 6371000; // meters
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
              Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

// Helper to encode a path into Google Polyline format
function encodePolyline(coords) {
    let result = '';
    let prevLat = 0;
    let prevLng = 0;

    for (let i = 0; i < coords.length; i++) {
        let lat = Math.round(coords[i][0] * 1e5);
        let lng = Math.round(coords[i][1] * 1e5);

        let dLat = lat - prevLat;
        let dLng = lng - prevLng;

        result += encodeValue(dLat);
        result += encodeValue(dLng);

        prevLat = lat;
        prevLng = lng;
    }
    return result;
}

function encodeValue(value) {
    value = value < 0 ? ~(value << 1) : value << 1;
    let result = '';
    while (value >= 0x20) {
        result += String.fromCharCode(((value & 0x1f) | 0x20) + 63);
        value >>= 5;
    }
    result += String.fromCharCode(value + 63);
    return result;
}

// OSRM Free Routing API (provides actual road routing without API Key)
async function getOSRMRoute(originLat, originLng, destLat, destLng) {
    try {
        const url = `http://router.project-osrm.org/route/v1/driving/` +
            `${originLng},${originLat};${destLng},${destLat}` +
            `?overview=full&geometries=polyline`;
            
        const response = await axios.get(url);
        const data = response.data;
        
        if (data.code === 'Ok' && data.routes && data.routes.length > 0) {
            const route = data.routes[0];
            const distKm = route.distance / 1000;
            const durationMin = Math.max(1, Math.round(route.duration / 60));
            
            return {
                status: 'OK',
                encodedPolyline: route.geometry,
                distance: `${distKm.toFixed(1)} km`,
                duration: `${durationMin} phút`,
                isOSRM: true
            };
        }
    } catch (err) {
        console.error('OSRM fallback error:', err.message);
    }
    return null;
}

router.get('/directions', async (req, res) => {
    const { originLat, originLng, destLat, destLng } = req.query;

    // Validate đầu vào
    if (!originLat || !originLng || !destLat || !destLng) {
        return res.status(400).json({ error: 'Thiếu tham số tọa độ' });
    }

    const originLatNum = parseFloat(originLat);
    const originLngNum = parseFloat(originLng);
    const destLatNum = parseFloat(destLat);
    const destLngNum = parseFloat(destLng);

    try {
        const googleUrl = `https://maps.googleapis.com/maps/api/directions/json` +
            `?origin=${originLat},${originLng}` +
            `&destination=${destLat},${destLng}` +
            `&mode=driving` +
            `&language=vi` +
            `&key=${DIRECTIONS_API_KEY}`;

        const response = await axios.get(googleUrl);
        const data = response.data;

        if (data.status !== 'OK') {
            console.error(`Google Directions API error status: ${data.status}. Message: ${data.error_message || 'None'}`);
            console.log("Trying OSRM fallback...");
            const osrmRoute = await getOSRMRoute(originLatNum, originLngNum, destLatNum, destLngNum);
            if (osrmRoute) {
                console.log("OSRM fallback successful!");
                return res.json(osrmRoute);
            }
            console.log("OSRM failed. Falling back to straight-line route...");
            return getMockedRouteResponse(res, originLatNum, originLngNum, destLatNum, destLngNum, data.error_message);
        }

        // Chỉ trả về phần cần thiết, không expose toàn bộ response Google
        const route = data.routes[0];
        return res.json({
            status: 'OK',
            encodedPolyline: route.overview_polyline.points,
            distance: route.legs[0].distance.text,
            duration: route.legs[0].duration.text
        });

    } catch (error) {
        console.error('Directions API error:', error.message);
        console.log("Error occurred. Trying OSRM fallback...");
        const osrmRoute = await getOSRMRoute(originLatNum, originLngNum, destLatNum, destLngNum);
        if (osrmRoute) {
            console.log("OSRM fallback successful!");
            return res.json(osrmRoute);
        }
        console.log("OSRM failed. Falling back to straight-line route...");
        return getMockedRouteResponse(res, originLatNum, originLngNum, destLatNum, destLngNum, error.message);
    }
});

function getMockedRouteResponse(res, originLat, originLng, destLat, destLng, message) {
    const mockedEncoded = encodePolyline([
        [originLat, originLng],
        [destLat, destLng]
    ]);
    const distMeters = getDistance(originLat, originLng, destLat, destLng);
    const distKm = distMeters / 1000;
    
    // Giả lập tốc độ lái xe trung bình là 30km/h (tức là 2 phút cho 1km)
    const durationMin = Math.max(1, Math.round(distKm * 2));

    return res.json({
        status: 'OK',
        encodedPolyline: mockedEncoded,
        distance: `${distKm.toFixed(1)} km`,
        duration: `${durationMin} phút`,
        isMocked: true,
        mockReason: message || 'Google API Error Fallback'
    });
}

module.exports = router;