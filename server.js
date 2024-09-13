import fetch from 'node-fetch';
import { writeFile } from 'fs/promises';

let servers = [
    ".accstorefront-7c6896c975-rtb2k",
    ".accstorefront-7c6896c975-cnw5p",
    ".accstorefront-7c6896c975-tgdgp"
];

async function downloadPage(server, url) {

    try {
        const response = await fetch("https://www.lkbennett.com/", {
            "headers": {
                "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                "accept-language": "en,en-US;q=0.9",
                "cache-control": "max-age=0",
                "priority": "u=0, i",
                "sec-ch-ua": "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Google Chrome\";v=\"128\"",
                "upgrade-insecure-requests": "1",
                "cookie": "ROUTE=" + server,
                "Referer": "https://www.lkbennett.com/login",
                "Referrer-Policy": "strict-origin-when-cross-origin"
            },
            "body": null,
            "method": "GET"
        });

        const html = await response.text();

        // var cookies = response.headers.get('set-cookie');
        // console.log("cookies", server, cookies.split("; "));
        
        const fileName = "/home/paulograbin/Desktop/download_script/results/page_" + server + new Date() + ".html";

        // Save the HTML to a file (optional)
        await writeFile(fileName, html);
        console.log('HTML saved successfully!');

        // Or you can just log it to the console
        // console.log(html);
    } catch (error) {
        console.error('Error fetching the page:', error);

        const fileName = "error_page_" + new Date().getTime();
        await writeFile(fileName, html);
    }
}

let url = 'https://lkbennett.com';

for(let server of servers) {
    console.log("server", server);
    downloadPage(server, url);
}


