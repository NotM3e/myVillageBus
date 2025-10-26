package com.myvillagebus

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.myvillagebus.navigation.NavGraph
import com.myvillagebus.ui.theme.BusScheduleTheme
import com.myvillagebus.ui.viewmodel.BusViewModel
import com.myvillagebus.ui.viewmodel.BusViewModelFactory
import com.myvillagebus.data.model.BusSchedule
import com.myvillagebus.data.model.BusStop
import com.myvillagebus.utils.CsvImporter
import java.time.DayOfWeek

class MainActivity : ComponentActivity() {

    private fun testSync() {
        // URL do Twojego arkusza Config (zastąp GID prawdziwym)
        val configUrl = "https://docs.google.com/spreadsheets/d/e/2PACX-1vSUpEKaD5spMbQ0e_VVj2XI1pxlTbGz6QV5AEvD0HQIM-xDk1yzhWA3yo7zwPjJ8yq9anAJrixPn4WI/pub?gid=0&single=true&output=tsv"

        viewModel.syncWithGoogleSheets(configUrl)
    }

    // Inicjalizacja ViewModelu z Repository
    private val viewModel: BusViewModel by viewModels {
        BusViewModelFactory((application as BusScheduleApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicjalizacja przykładowych danych (tylko przy pierwszym uruchomieniu)
        viewModel.initializeSampleData(getSampleSchedules())

         testSync()  // Odkomentuj aby przetestować synchronizację

        setContent {
            BusScheduleTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }
    }

    // Przykładowe dane
    private fun getSampleSchedules(): List<BusSchedule> {
        return listOf(
            BusSchedule(
                id = 0,
                carrierName = "Trans-Bus",
                lineDesignation = "A",
                designationDescription = "Kurs w dni powszednie",
                busLine = "Dworzec - Osiedle Słoneczne",
                departureTime = "08:30",
                stopName = "Dworzec Główny",
                direction = "Osiedle Słoneczne",
                operatingDays = BusSchedule.weekdaysList(),
                stops = listOf(
                    BusStop("Dworzec Główny", "08:30", 0),
                    BusStop("Plac Centralny", "08:35", 0),
                    BusStop("Szkoła Podstawowa", "08:40", 0),
                    BusStop("Przychodnia", "08:45", 0),
                    BusStop("Osiedle Słoneczne", "08:52", 0)
                )
            ),
            BusSchedule(
                id = 0,
                carrierName = "Trans-Bus",
                lineDesignation = "B",
                designationDescription = "Kurs weekendowy i świąteczny",
                busLine = "Dworzec - Osiedle Słoneczne",
                departureTime = "09:15",
                stopName = "Dworzec Główny",
                direction = "Osiedle Słoneczne",
                operatingDays = BusSchedule.weekendsList(),
                stops = listOf(
                    BusStop("Dworzec Główny", "09:15", 0),
                    BusStop("Plac Centralny", "09:20", 2),
                    BusStop("Szkoła Podstawowa", "09:27", 2),
                    BusStop("Przychodnia", "09:32", 2),
                    BusStop("Osiedle Słoneczne", "09:39", 2)
                )
            ),
            BusSchedule(
                id = 0,
                carrierName = "Komfort-Express",
                lineDesignation = "1",
                designationDescription = "Linia standardowa",
                busLine = "Centrum - Szpital",
                departureTime = "10:00",
                stopName = "Plac Centralny",
                direction = "Szpital",
                operatingDays = BusSchedule.weekdaysList(),
                stops = listOf(
                    BusStop("Plac Centralny", "10:00", 0),
                    BusStop("Ratusz", "10:05", 0),
                    BusStop("Park Miejski", "10:12", 0),
                    BusStop("Politechnika", "10:18", 0),
                    BusStop("Szpital Wojewódzki", "10:25", 0)
                )
            ),
            BusSchedule(
                id = 0,
                carrierName = "Komfort-Express",
                lineDesignation = "1 Express",
                designationDescription = "Kurs przyśpieszony",
                busLine = "Centrum - Szpital",
                departureTime = "10:45",
                stopName = "Plac Centralny",
                direction = "Szpital",
                operatingDays = DayOfWeek.values().toList(),  // ← POPRAWIONE: Codziennie
                stops = listOf(
                    BusStop("Plac Centralny", "10:45", 0),
                    BusStop("Ratusz", "10:50", 0),
                    BusStop("Park Miejski", "10:57", 0),
                    BusStop("Politechnika", "11:03", 0),
                    BusStop("Szpital Wojewódzki", "11:10", 0)
                )
            ),
            BusSchedule(
                id = 0,
                carrierName = "PKS Lokal",
                lineDesignation = "N",
                designationDescription = "Kurs nocny",
                busLine = "Lotnisko - Dworzec",
                departureTime = "14:20",
                stopName = "Lotnisko",
                direction = "Dworzec Główny",
                operatingDays = listOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.FRIDAY
                ),
                stops = listOf(
                    BusStop("Lotnisko", "14:20", 0),
                    BusStop("Galeria Handlowa", "14:30", 0),
                    BusStop("Stadion", "14:38", 0),
                    BusStop("Plac Centralny", "14:45", 0),
                    BusStop("Dworzec Główny", "14:52", 0)
                )
            ),
            BusSchedule(
                id = 0,
                carrierName = "Trans-Bus",
                lineDesignation = null,
                designationDescription = null,
                busLine = "Dworzec - Park",
                departureTime = "15:00",
                stopName = "Dworzec Główny",
                direction = "Park Miejski",
                operatingDays = listOf(DayOfWeek.SATURDAY),
                stops = listOf(
                    BusStop("Dworzec Główny", "15:00", 0),
                    BusStop("Plac Centralny", "15:05", 0),
                    BusStop("Park Miejski", "15:12", 0)
                )
            )
        )
    }
}