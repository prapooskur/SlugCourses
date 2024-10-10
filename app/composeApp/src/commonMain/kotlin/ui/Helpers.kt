package ui

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

private const val TAG = "Helpers"

//fun shortToast(text: String, context: Context) {
//    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
//}

val supabaseClientInstance: SupabaseClient by lazy {
    createSupabaseClient(
        supabaseUrl = "https://wavqdavgfeimdhcabzhf.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndhdnFkYXZnZmVpbWRoY2FiemhmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mjg1MDk5MzMsImV4cCI6MjA0NDA4NTkzM30.Tz1AbHupgRu3xG7aejKJvxoEPtxEwqXj5dFBG9zISVA"
    ) {
        install(Postgrest)
    }
}

fun getSupabaseClient(): SupabaseClient {
    return supabaseClientInstance
}

// list of departments
val departmentList = setOf(
    "APLX",
    "AM",
    "ARBC",
    "ART",
    "ARTG",
    "ASTR",
    "BIOC",
    "BIOL",
    "BIOE",
    "BME",
    "CRSN",
    "CHEM",
    "CHIN",
    "CSP",
    "CLNI",
    "CMMU",
    "CMPM",
    "CSE",
    "COWL",
    "CRES",
    "CRWN",
    "DANM",
    "EART",
    "ECON",
    "EDUC",
    "ECE",
    "ESCI",
    "ENVS",
    "FMST",
    "FILM",
    "FREN",
    "GAME",
    "GERM",
    "GCH",
    "GRAD",
    "GREE",
    "HEBR",
    "HIS",
    "HAVC",
    "HISC",
    "HCI",
    "HUMN",
    "ITAL",
    "JAPN",
    "JRLC",
    "KRSG",
    "LAAD",
    "LATN",
    "LALS",
    "LGST",
    "LING",
    "LIT",
    "MATH",
    "MERR",
    "METX",
    "MUSC",
    "NLP",
    "OAKS",
    "OCEA",
    "PERS",
    "PHIL",
    "PBS",
    "PHYE",
    "PHYS",
    "POLI",
    "PRTR",
    "PORT",
    "PSYC",
    "SCIC",
    "SOCD",
    "SOCY",
    "SPAN",
    "SPHS",
    "STAT",
    "STEV",
    "TIM",
    "THEA",
    "UCDC",
    "VAST",
    "WRIT"
)