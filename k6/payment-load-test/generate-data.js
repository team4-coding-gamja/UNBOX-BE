const fs = require('fs');
const crypto = require('crypto');

function getMd5Uuid(prefix) {
    const hash = crypto.createHash('md5').update(prefix).digest('hex');
    return `${hash.substring(0, 8)}-${hash.substring(8, 12)}-${hash.substring(12, 16)}-${hash.substring(16, 20)}-${hash.substring(20, 32)}`;
}

const data = [];
const SEED_START = 200001;
const SEED_END = 230000;

for (let gs = SEED_START; gs <= SEED_END; gs++) {
    data.push({
        paymentId: getMd5Uuid('pay-' + gs),
        orderId: getMd5Uuid('ord-' + gs),
        buyerId: 1 + (gs % 50),
        paymentKey: `test_success_${gs}`,
        amount: 10000
    });
}

const outputPath = __dirname + '/data.json';
fs.writeFileSync(outputPath, JSON.stringify(data, null, 2));

console.log(`✅ ${data.length} records successfully generated at ${outputPath}`);
