import fs from 'fs';
const filePath = 'app/src/main/java/pl/siatkalive/tlk/data/TauronNetwork.kt';
let code = fs.readFileSync(filePath, 'utf8');

const startIdx = code.indexOf('data class StandingRowDto');
if (startIdx !== -1) {
    const endIdx = code.indexOf(')', startIdx) + 1;
    const newClass = `data class StandingRowDto(
    val position: Int? = 0,
    val teamId: Int? = 0,
    val name: String = "",
    val shortName: String = "",
    val matchesPlayed: Int? = 0,
    val points: Int? = 0,
    val matchesWon: Int? = 0,
    val matchesLost: Int? = 0,
    val setsWon: Int? = 0,
    val setsLost: Int? = 0
)`;
    code = code.substring(0, startIdx) + newClass + code.substring(endIdx);
    fs.writeFileSync(filePath, code, 'utf8');
    console.log("✅ Plik zaktualizowany!");
} else {
    console.log("❌ Nie znaleziono klasy StandingRowDto!");
}
