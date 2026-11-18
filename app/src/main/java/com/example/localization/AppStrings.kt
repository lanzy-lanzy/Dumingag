package com.example.localization

import com.example.model.AppLanguage
import com.example.model.AttractionCategory
import com.example.model.TransitModeCategory
import com.example.model.TransitServiceStatus
import com.example.model.TravelMode

object AppStrings {
    fun navExplore(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Explore"
        AppLanguage.CEB -> "Suroy"
        AppLanguage.TL -> "Tuklasin"
        AppLanguage.ES -> "Explorar"
    }

    fun navMap(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Map"
        AppLanguage.CEB -> "Mapa"
        AppLanguage.TL -> "Mapa"
        AppLanguage.ES -> "Mapa"
    }

    fun navTransit(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Transit"
        AppLanguage.CEB -> "Sakyanan"
        AppLanguage.TL -> "Biyahe"
        AppLanguage.ES -> "Tránsito"
    }

    fun navPlan(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Plan"
        AppLanguage.CEB -> "Plano"
        AppLanguage.TL -> "Plano"
        AppLanguage.ES -> "Plan"
    }

    fun navReviews(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Reviews"
        AppLanguage.CEB -> "Rebyu"
        AppLanguage.TL -> "Puna"
        AppLanguage.ES -> "Reseñas"
    }

    fun welcomeTagline(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Organic Valley of Zamboanga del Sur"
        AppLanguage.CEB -> "Organikong Walog sa Zamboanga del Sur"
        AppLanguage.TL -> "Organikong Lambak ng Zamboanga del Sur"
        AppLanguage.ES -> "Valle Orgánico de Zamboanga del Sur"
    }

    fun searchHint(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Search attractions, landmarks, transit..."
        AppLanguage.CEB -> "Pangita og lugar, simbahan, falls, sakyanan..."
        AppLanguage.TL -> "Maghanap ng atraksyon, ruta, o gabay..."
        AppLanguage.ES -> "Buscar atracciones, hitos, transporte..."
    }

    fun categoryLabel(cat: AttractionCategory, lang: AppLanguage) = when (cat) {
        AttractionCategory.ALL -> when (lang) {
            AppLanguage.EN -> "All"
            AppLanguage.CEB -> "Tanan"
            AppLanguage.TL -> "Lahat"
            AppLanguage.ES -> "Todo"
        }
        AttractionCategory.HISTORICAL -> when (lang) {
            AppLanguage.EN -> "Historical"
            AppLanguage.CEB -> "Kasaysayan"
            AppLanguage.TL -> "Makasaysayan"
            AppLanguage.ES -> "Histórico"
        }
        AttractionCategory.NATURE -> when (lang) {
            AppLanguage.EN -> "Nature"
            AppLanguage.CEB -> "Kinaiyahan"
            AppLanguage.TL -> "Kalikasan"
            AppLanguage.ES -> "Naturaleza"
        }
        AttractionCategory.CULTURAL -> when (lang) {
            AppLanguage.EN -> "Cultural"
            AppLanguage.CEB -> "Kultura"
            AppLanguage.TL -> "Kultura"
            AppLanguage.ES -> "Cultural"
        }
        AttractionCategory.LANDMARK -> when (lang) {
            AppLanguage.EN -> "Landmarks"
            AppLanguage.CEB -> "Mga Landmark"
            AppLanguage.TL -> "Palatandaan"
            AppLanguage.ES -> "Hitos"
        }
        AttractionCategory.ORGANIC_FARM -> when (lang) {
            AppLanguage.EN -> "Organic Farms"
            AppLanguage.CEB -> "Organikong Uma"
            AppLanguage.TL -> "Bukid"
            AppLanguage.ES -> "Granjas"
        }
    }

    fun travelModeLabel(mode: TravelMode, lang: AppLanguage) = when (mode) {
        TravelMode.WALK -> when (lang) {
            AppLanguage.EN -> "Walk"
            AppLanguage.CEB -> "Lakaw"
            AppLanguage.TL -> "Lakad"
            AppLanguage.ES -> "A pie"
        }
        TravelMode.HABAL_HABAL -> when (lang) {
            AppLanguage.EN -> "Habal-Habal"
            AppLanguage.CEB -> "Habal-Habal"
            AppLanguage.TL -> "Habal-Habal"
            AppLanguage.ES -> "Mototaxi"
        }
        TravelMode.JEEPNEY_CAR -> when (lang) {
            AppLanguage.EN -> "Van / Car"
            AppLanguage.CEB -> "Van / Sakyanan"
            AppLanguage.TL -> "Van / Kotse"
            AppLanguage.ES -> "Auto / Van"
        }
    }

    fun weatherTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Dumingag Weather"
        AppLanguage.CEB -> "Panahon sa Dumingag"
        AppLanguage.TL -> "Panahon sa Dumingag"
        AppLanguage.ES -> "Clima en Dumingag"
    }

    fun threeDayForecastTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "3-Day Forecast"
        AppLanguage.CEB -> "3 ka Adlaw nga Panahon"
        AppLanguage.TL -> "3-Araw na Pagtataya"
        AppLanguage.ES -> "Pronóstico de 3 Días"
    }

    fun weatherConditionText(code: Int, lang: AppLanguage): String = when (code) {
        0 -> when (lang) {
            AppLanguage.EN -> "Clear highland sky"
            AppLanguage.CEB -> "Hayag ang kalangitan"
            AppLanguage.TL -> "Maaliwalas ang langit"
            AppLanguage.ES -> "Cielo despejado"
        }
        in 1..3 -> when (lang) {
            AppLanguage.EN -> "Partly cloudy valley breeze"
            AppLanguage.CEB -> "Madag-umon apan hayahay"
            AppLanguage.TL -> "Bahagyang maulap"
            AppLanguage.ES -> "Parcialmente nublado"
        }
        in 45..48 -> when (lang) {
            AppLanguage.EN -> "Morning mountain mist"
            AppLanguage.CEB -> "Gabon sa kabukiran"
            AppLanguage.TL -> "Hamog sa kabundukan"
            AppLanguage.ES -> "Niebla de montaña"
        }
        in 51..67, in 80..82 -> when (lang) {
            AppLanguage.EN -> "Tropical rain showers"
            AppLanguage.CEB -> "Nag-ulan sa kabukiran"
            AppLanguage.TL -> "Pag-ulan sa lugar"
            AppLanguage.ES -> "Lluvias tropicales"
        }
        in 95..99 -> when (lang) {
            AppLanguage.EN -> "Thunderstorm advisory"
            AppLanguage.CEB -> "Pasidaan sa kilat ug ulan"
            AppLanguage.TL -> "Babala ng pagkidlat"
            AppLanguage.ES -> "Alerta de tormenta"
        }
        else -> when (lang) {
            AppLanguage.EN -> "Mild valley conditions"
            AppLanguage.CEB -> "Kalmado ang panahon"
            AppLanguage.TL -> "Katamtamang panahon"
            AppLanguage.ES -> "Clima templado"
        }
    }

    fun weatherTravelAdvisory(code: Int, lang: AppLanguage): String = when {
        code >= 80 -> when (lang) {
            AppLanguage.EN -> "Slippery upland trails near Mt. Timolan & Danlugan. Prefer paved Poblacion landmarks."
            AppLanguage.CEB -> "Dangog ang mga dalan paingon sa Mt. Timolan ug Danlugan. Pag-amping sa byahe."
            AppLanguage.TL -> "Madulas ang daan sa Mt. Timolan at Danlugan. Mag-ingat sa pag-akyat."
            AppLanguage.ES -> "Senderos resbaladizos cerca de Mt. Timolan. Precaución en rutas de montaña."
        }
        code in 51..67 -> when (lang) {
            AppLanguage.EN -> "Bring a light poncho for upland eco-tours; valley farms remain accessible."
            AppLanguage.CEB -> "Pagdala og kapote para sa bukid; maagian ra ang mga uma sa walog."
            AppLanguage.TL -> "Magdala ng kapote; madaling puntahan ang mga bukid sa lambak."
            AppLanguage.ES -> "Lleve impermeable ligero para las rutas de montaña."
        }
        else -> when (lang) {
            AppLanguage.EN -> "Ideal conditions for waterfalls, rice terrace walks, and heritage plaza sightseeing."
            AppLanguage.CEB -> "Maayong panahon para mosuroy sa falls, humayan, ug plaza."
            AppLanguage.TL -> "Magandang panahon para sa pagbisita sa talon, bukid, at plaza."
            AppLanguage.ES -> "Condiciones ideales para cascadas, terrazas y plaza histórica."
        }
    }

    fun onlineLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Online"
        AppLanguage.CEB -> "Konektado"
        AppLanguage.TL -> "Online"
        AppLanguage.ES -> "En línea"
    }

    fun offlineLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Offline Mode"
        AppLanguage.CEB -> "Offline Mode"
        AppLanguage.TL -> "Offline Mode"
        AppLanguage.ES -> "Modo Sin Conexión"
    }

    fun navigateOffline(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Navigate Offline"
        AppLanguage.CEB -> "Giya sa Ruta (Offline)"
        AppLanguage.TL -> "Gabay sa Ruta (Offline)"
        AppLanguage.ES -> "Navegar Sin Conexión"
    }

    fun addToItinerary(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Add to Itinerary"
        AppLanguage.CEB -> "Idugang sa Plano"
        AppLanguage.TL -> "Idagdag sa Plano"
        AppLanguage.ES -> "Añadir al Itinerario"
    }

    fun saveOffline(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Save Offline"
        AppLanguage.CEB -> "Tipigi (Offline)"
        AppLanguage.TL -> "I-save (Offline)"
        AppLanguage.ES -> "Guardar Sin Conexión"
    }

    fun savedOffline(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Saved Offline"
        AppLanguage.CEB -> "Natipig Na"
        AppLanguage.TL -> "Naka-save Na"
        AppLanguage.ES -> "Guardado"
    }

    fun emergencyTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Emergency & Safety Contacts"
        AppLanguage.CEB -> "Mga Numero sa Emerhensya"
        AppLanguage.TL -> "Mga Numero sa Emerhensya"
        AppLanguage.ES -> "Contactos de Emergencia"
    }

    fun transitTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Local Transit & Fare Guide"
        AppLanguage.CEB -> "Sakyanan ug Pletehan"
        AppLanguage.TL -> "Lokal na Transportasyon"
        AppLanguage.ES -> "Transporte Local y Tarifas"
    }

    fun alertsSectionTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Travel Alerts & Push Reminders"
        AppLanguage.CEB -> "Mga Pahibalo sa Byahe"
        AppLanguage.TL -> "Mga Alerto sa Paglalakbay"
        AppLanguage.ES -> "Alertas y Recordatorios"
    }

    fun syncToCalendar(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Sync to Calendar"
        AppLanguage.CEB -> "I-sync sa Kalendaryo"
        AppLanguage.TL -> "I-sync sa Kalendaryo"
        AppLanguage.ES -> "Sincronizar Calendario"
    }

    fun offlineVaultTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Offline Maps & Saved Documents"
        AppLanguage.CEB -> "Offline Mapa ug Dokumento"
        AppLanguage.TL -> "Offline Mapa at Dokumento"
        AppLanguage.ES -> "Mapas y Documentos Offline"
    }

    fun communityReviewsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.EN -> "Traveler Community Reviews"
        AppLanguage.CEB -> "Mga Rebyu sa Bisita"
        AppLanguage.TL -> "Mga Review ng Manlalakbay"
        AppLanguage.ES -> "Reseñas de la Comunidad"
    }

    fun transitModeLabel(category: TransitModeCategory, lang: AppLanguage) = when (category) {
        TransitModeCategory.TRICYCLE -> when (lang) {
            AppLanguage.EN -> "Tricycle / Bao-Bao"
            AppLanguage.CEB -> "Traysikol / Bao-Bao"
            AppLanguage.TL -> "Traysikel / Bao-Bao"
            AppLanguage.ES -> "Triciclo / Bao-Bao"
        }
        TransitModeCategory.HABAL_HABAL -> when (lang) {
            AppLanguage.EN -> "Habal-Habal"
            AppLanguage.CEB -> "Habal-Habal"
            AppLanguage.TL -> "Habal-Habal"
            AppLanguage.ES -> "Mototaxi"
        }
        TransitModeCategory.BUS_VAN -> when (lang) {
            AppLanguage.EN -> "Bus & Van"
            AppLanguage.CEB -> "Bus ug Van"
            AppLanguage.TL -> "Bus at Van"
            AppLanguage.ES -> "Autobús y Van"
        }
        TransitModeCategory.JEEPNEY_MULTICAB -> when (lang) {
            AppLanguage.EN -> "Multicab / Jeep"
            AppLanguage.CEB -> "Multicab / Jeep"
            AppLanguage.TL -> "Multicab / Dyip"
            AppLanguage.ES -> "Multicab / Jeepney"
        }
    }

    fun transitStatusLabel(status: TransitServiceStatus, lang: AppLanguage) = when (status) {
        TransitServiceStatus.OPERATIONAL -> when (lang) {
            AppLanguage.EN -> "Operational"
            AppLanguage.CEB -> "Aktibo"
            AppLanguage.TL -> "Aktibo"
            AppLanguage.ES -> "Operativo"
        }
        TransitServiceStatus.LIMITED -> when (lang) {
            AppLanguage.EN -> "Limited Trips"
            AppLanguage.CEB -> "Limitado ang Biyahe"
            AppLanguage.TL -> "Limitadong Biyahe"
            AppLanguage.ES -> "Servicio Limitado"
        }
        TransitServiceStatus.ADVISORY -> when (lang) {
            AppLanguage.EN -> "Travel Advisory"
            AppLanguage.CEB -> "May Pahimangno"
            AppLanguage.TL -> "May Abiso"
            AppLanguage.ES -> "Aviso de Ruta"
        }
    }
}

