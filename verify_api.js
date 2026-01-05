
const https = require('https');

const apiKey = '23c9561ec76e479fa72778d73ff629f6';
// Test 1: Try title: syntax
// const keyword = encodeURIComponent('title:"야구"'); 
// Test 2: Try filtering in code? No, test API capability first.
const keyword = encodeURIComponent('title:"야구"'); 
const dateFrom = '2025-01-02';

const url = `https://api-v2.deepsearch.com/v1/articles?keyword=${keyword}&sort=desc&uniquify=true&date_from=${dateFrom}&page=1&page_size=10&api_key=${apiKey}`;

console.log('Testing URL:', url);

https.get(url, (res) => {
    let data = '';
    res.on('data', (chunk) => data += chunk);
    res.on('end', () => {
        try {
            const json = JSON.parse(data);
            console.log('Total items:', json.total_items);
            
            if (json.data && json.data.length > 0) {
                console.log('\nTop 5 Articles:');
                json.data.slice(0, 5).forEach((item, index) => {
                    console.log(`${index + 1}. [${item.title}] (${item.publisher}) - ${item.published_at}`);
                });

                // Check sorting
                const dates = json.data.map(item => new Date(item.published_at).getTime());
                const isSorted = dates.every((d, i) => i === 0 || d <= dates[i - 1]);
                console.log('\nIs sorted by date (desc)?', isSorted);

                // Check "먹보족사장" (irrelevant result)
                const irrelevant = json.data.find(item => item.title.includes('먹보족') || item.title.includes('사장'));
                if (irrelevant) {
                    console.log('\nFound suspicious irrelevant article:', irrelevant.title);
                } else {
                    console.log('\nNo "먹보족/사장" articles found in top 10.');
                }

                // Check for duplicates in this batch
                const titles = json.data.map(item => item.title);
                const uniqueTitles = new Set(titles);
                console.log(`\nDuplicates in batch? ${titles.length !== uniqueTitles.size ? 'YES' : 'NO'} (Size: ${titles.length} vs Unique: ${uniqueTitles.size})`);

            } else {
                console.log('No data found.');
            }
        } catch (e) {
            console.log('Error:', e.message);
            console.log('Raw:', data.substring(0, 500));
        }
    });
}).on('error', (err) => {
    console.log('Request Error:', err.message);
});
