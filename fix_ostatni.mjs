import fs from 'fs';
const filePath = 'app/src/main/java/pl/siatkalive/tlk/data/TauronNetwork.kt';
let code = fs.readFileSync(filePath, 'utf8');

// Czyścimy błędy po poprzednim sedzie
code = code.replace(/= 0 = 0/g, '= 0');
// Wymuszamy typy, których żąda MainActivity
code = code.replace(/val name: String\? = ""/g, 'val name: String = ""');
code = code.replace(/val shortName: String,/g, 'val shortName: String = "",');
code = code.replace(/val shortName: String\? = "",/g, 'val shortName: String = "",');

fs.writeFileSync(filePath, code, 'utf8');
