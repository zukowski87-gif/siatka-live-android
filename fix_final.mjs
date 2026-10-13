import fs from 'fs';
const filePath = 'app/src/main/java/pl/siatkalive/tlk/data/TauronNetwork.kt';
let code = fs.readFileSync(filePath, 'utf8');

const startIdx = code.indexOf('data class StandingRowDto');
let endIdx = -1;
let openParens = 0;
for (let i = startIdx; i < code.length; i++) {
    if (code[i] === '(') openParens++;
    if (code[i] === ')') {
        openParens--;
        if (openParens === 0) {
            endIdx = i + 1;
            break;
        }
    }
}

if (startIdx !== -1 && endIdx !== -1) {
    let body = code.substring(startIdx, endIdx);
    // Dodaje = 0 lub = "" do typów, jeśli ich nie mają, nie psując adnotacji @Json
    body = body.replace(/:\s*Int\s*,/g, ': Int = 0,');
    body = body.replace(/:\s*Int\s*\)/g, ': Int = 0)');
    body = body.replace(/:\s*String\s*,/g, ': String = "",');
    body = body.replace(/:\s*String\s*\)/g, ': String = "")');
    
    code = code.substring(0, startIdx) + body + code.substring(endIdx);
    fs.writeFileSync(filePath, code, 'utf8');
}
