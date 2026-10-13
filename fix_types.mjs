import fs from 'fs';
const filePath = 'app/src/main/java/pl/siatkalive/tlk/data/TauronNetwork.kt';
let code = fs.readFileSync(filePath, 'utf8');

code = code.replace(/data class StandingRowDto\s*\([\s\S]*?\)/, `data class StandingRowDto(
    val position: Int? = 0,
    val teamId: Int? = null,
    val name: String? = "",
    val shortName: String = "",
    val matchesPlayed: Int? = 0,
    val points: Int? = 0,
    val matchesWon: Int? = 0,
    val matchesLost: Int? = 0,
    val setsWon: Int = 0,
    val setsLost: Int = 0
)`);
fs.writeFileSync(filePath, code, 'utf8');
