import fs from 'fs';
const filePath = 'app/src/main/java/pl/siatkalive/tlk/data/TauronNetwork.kt';
let code = fs.readFileSync(filePath, 'utf8');

// 1. Usuwamy potworki "= 0 = 0", które zrobił wcześniej mój błędny sed
code = code.replace(/= 0 = 0/g, '= 0');

// 2. Naprawiamy typy: MainActivity krzyczał, że potrzebuje czystego 'String', a nie 'String?'
code = code.replace(/val name: String\? = ""/g, 'val name: String = ""');
code = code.replace(/val shortName: String,/g, 'val shortName: String = "",');

fs.writeFileSync(filePath, code, 'utf8');
