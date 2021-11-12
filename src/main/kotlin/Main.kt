import kotlinx.cli.*
import kotlinx.serialization.json.*
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToLong

val dateTimeFormatterDay: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun main(args: Array<String>) {

    val parser = ArgParser("watsonStat")

    val mondayWorkingHours by parser.option(ArgType.Double, description = "Working hours on monday").default(8.0)
    val tuesdayWorkingHours by parser.option(ArgType.Double, description = "Working hours on tuesday").default(8.0)
    val wednesdayWorkingHours by parser.option(ArgType.Double, description = "Working hours on wednesday").default(8.0)
    val thursdayWorkingHours by parser.option(ArgType.Double, description = "Working hours on thursday").default(8.0)
    val fridayWorkingHours by parser.option(ArgType.Double, description = "Working hours on friday").default(8.0)
    val saturdayWorkingHours by parser.option(ArgType.Double, description = "Working hours on saturday").default(0.0)
    val sundayWorkingHours by parser.option(ArgType.Double, description = "Working hours on sunday").default(0.0)

    val operation by parser.argument(
        ArgType.Choice(listOf("balance", "list"), { it }),
        fullName = "operation",
        description = "Operation to perform on the data"
    )
    val framesPath by parser.argument(ArgType.String, fullName = "frames", description = "Path of frames file")
    val startDateString by parser.argument(
        ArgType.String,
        fullName = "start date",
        description = "start date for evaluation, format YYYY-mm-dd"
    )
    val endDateString by parser.argument(
        ArgType.String,
        fullName = "end date",
        description = "end date of evaluation, format YYYY-mm-dd, now if not specified"
    ).optional()

    parser.parse(args)

    val workingHours = listOf(
        mondayWorkingHours,
        tuesdayWorkingHours,
        wednesdayWorkingHours,
        thursdayWorkingHours,
        fridayWorkingHours,
        saturdayWorkingHours,
        sundayWorkingHours,
    )

    val endDate = if (endDateString == null) {
        LocalDate.now()
    } else {
        LocalDate.parse(endDateString)
    }
    val startDate = LocalDate.parse(startDateString)

    val days = startDate.daysBetween(endDate, workingHours)

    val jsonString: String = File(framesPath).readText(Charsets.UTF_8)
    val jsonArray = Json.parseToJsonElement(jsonString)
    jsonArray.jsonArray
        .filter { it.jsonArray.size == 6 }
        .forEach {
            val tStart = it.jsonArray[0].jsonPrimitive.long
            val date = Instant.ofEpochMilli(tStart * 1000).atZone(ZoneId.systemDefault()).toLocalDate()
            val day = days[date.format(dateTimeFormatterDay)]
            day?.addRecord(it.jsonArray)
        }

    if (operation == "list") {
        println("day, date, balance, projects, tags, identifier")
        days.values.sortedBy { it.date }.forEach { println(it.toString()) }
    } else if (operation == "balance") {
        val balance = days.values.sumOf { it.getBalance() }
        println("balance: ${balance.timeString()}")
    }
}

private fun Long.timeString(): String {
    val hours = this / 3600
    val minutes = abs((this % 3600) / 60)
    val seconds = abs(this % 60)

    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

private fun LocalDate.daysBetween(endDate: LocalDate, workingHours: List<Double>): HashMap<String, Day> {
    val days = HashMap<String, Day>()
    var currentDate = this
    do {
        days[currentDate.format(dateTimeFormatterDay)] = Day(currentDate, workingHours)
        currentDate = currentDate.plusDays(1)
    } while (currentDate < endDate)
    return days
}

class Day internal constructor(val date: LocalDate, workingHours: List<Double>) {
    private var target: Long = (workingHours[date.dayOfWeek.ordinal] * 60 * 60).roundToLong()

    private val records = mutableSetOf<Record>()

    fun addRecord(entry: JsonArray) {
        if (entry.size == 6) {
            val tStart = entry[0].jsonPrimitive.long
            val tEnd = entry[1].jsonPrimitive.long
            val project = entry[2].jsonPrimitive.toString()
            val tags = (entry[4] as Collection<JsonElement>).toTypedArray().map { it.toString() }
            val identifier = entry[3].jsonPrimitive.toString()
            records.add(Record(tEnd - tStart, project, tags, identifier))
        }
    }

    fun getBalance(): Long {
        return records.sumOf { it.time } - target
    }

    override fun toString(): String {
        return listOf<String>(
            date.dayOfWeek.name,
            date.format(DateTimeFormatter.ISO_LOCAL_DATE),
            getBalance().timeString(),
            records.map { it.project }.toSet().joinToString(separator = " "),
            records.map {it.tags}.flatten().toSet().joinToString(separator = " "),
            records.joinToString(separator = " ") { it.identifier }).joinToString()
    }

    class Record(val time: Long, val project: String, val tags: List<String>, val identifier: String){

        init {
            if(time < 0){
                println("Warning: negative time $time s recorded for entry $identifier.")
            }
        }

    }
}
