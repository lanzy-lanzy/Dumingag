package com.example.data

import com.example.R
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.AttractionCategory
import com.example.model.EmergencyContact
import com.example.model.LocalizedText
import com.example.model.NavigationStep
import com.example.model.OfflineMapEdge
import com.example.model.OfflineMapNode
import com.example.model.OfflineRoutePlan
import com.example.model.SavedDocument
import com.example.model.TransitMethod
import com.example.model.TransitModeCategory
import com.example.model.TransitRoute
import com.example.model.TransitServiceStatus
import com.example.model.TravelMode
import java.util.PriorityQueue
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object DumingagSeedData {

    // Town center Poblacion coordinates: 8.1650° N, 123.3465° E
    const val CENTER_LAT = 8.1650
    const val CENTER_LON = 123.3465

    val attractions: List<Attraction> = listOf(
        Attraction(
            id = "san_isidro_church",
            mapNodeId = "node_poblacion",
            name = LocalizedText(
                en = "San Isidro Labrador Parish & Rizal Plaza",
                ceb = "Simbahan sa San Isidro Labrador ug Plaza",
                tl = "Parokya ng San Isidro Labrador at Plaza",
                es = "Parroquia San Isidro Labrador y Plaza"
            ),
            subtitle = LocalizedText(
                en = "Historic Town Center & Heritage Promenade",
                ceb = "Sentro sa Kasaysayan sa Lungsod",
                tl = "Makasaysayang Sentro ng Bayan",
                es = "Centro Histórico y Paseo"
            ),
            description = LocalizedText(
                en = "The spiritual and civic heart of Dumingag Poblacion. Surrounded by century-old acacia trees, the plaza honors St. Isidore the Farmer alongside local agricultural heritage festivals.",
                ceb = "Ang sentro sa pagtuo ug kultura sa Dumingag Poblacion. Gipalibotan kini sa dagkong kahoy nga akasya ug duol sa munisipyo.",
                tl = "Ang sentro ng kasaysayan at kultura ng Dumingag Poblacion. Napapaligiran ng matatandang puno ng akasya at mga kapistahan sa pagsasaka.",
                es = "El corazón cívico y espiritual de Dumingag Poblacion, rodeado de acacias centenarias y arquitectura tradicional."
            ),
            historyNote = LocalizedText(
                en = "Established as Dumingag became an independent municipality in 1957, the parish anchors the annual Pasalamat Festival every May.",
                ceb = "Natukod samtang nahimong lungsod ang Dumingag niadtong 1957; sentro sa tinuig nga Pasalamat Festival matag Mayo.",
                tl = "Itinatag nang maging ganap na bayan ang Dumingag noong 1957; sentro ng taunang Pasalamat Festival tuwing Mayo.",
                es = "Fundada cuando Dumingag se convirtió en municipio en 1957; sede del Festival Pasalamat cada mayo."
            ),
            barangay = "Poblacion (San Pedro)",
            category = AttractionCategory.HISTORICAL,
            latitude = 8.1650,
            longitude = 123.3465,
            mapX = 0.50f,
            mapY = 0.52f,
            imageRes = R.drawable.img_heritage_plaza,
            openHours = "05:30 AM – 08:00 PM",
            entryFee = LocalizedText("Free entry", "Libre ang pagsulod", "Libreng pagpasok", "Entrada gratuita"),
            bestTime = LocalizedText("Early morning & golden hour", "Buntag sayo ug hapon", "Maagang umaga at dapithapon", "Amanecer y atardecer"),
            transitHint = LocalizedText(
                en = "Walking distance from Dumingag Integrated Bus Terminal.",
                ceb = "Madala ra og lakaw gikan sa Terminal sa Dumingag.",
                tl = "Nilalakad lamang mula sa Dumingag Terminal.",
                es = "A poca distancia a pie desde la terminal de autobuses."
            ),
            baseRating = 4.8
        ),
        Attraction(
            id = "paglinawan_eco_farm",
            mapNodeId = "node_libertad",
            name = LocalizedText(
                en = "Paglinawan Organic Eco-Farm",
                ceb = "Paglinawan Organic Eco-Farm",
                tl = "Paglinawan Organic Eco-Farm",
                es = "Granja Ecológica Paglinawan"
            ),
            subtitle = LocalizedText(
                en = "ATI-Accredited Organic Learning Site (dumingag.com)",
                ceb = "Sentro sa Organikong Panguma sa Libertad",
                tl = "Sentro ng Organikong Agrikultura sa Libertad",
                es = "Centro de Agricultura Orgánica Acreditado"
            ),
            description = LocalizedText(
                en = "Featured flagship eco-farm on dumingag.com in Purok 6, Brgy. Libertad. Explore chemical-free rice farming, natural composting, organic livestock integration, and rural food processing.",
                ceb = "Sikat nga organikong uma sa Brgy. Libertad nga gipasigarbo sa dumingag.com. Tan-awa ang natural nga pagpananom ug pagproseso sa pagkaon.",
                tl = "Pangunahing destinasyong agroturismo sa dumingag.com sa Brgy. Libertad na nagpapakita ng organikong pagsasaka at likas-kayang pagkain.",
                es = "Destino insignia de agroturismo en Purok 6, Brgy. Libertad, reconocido por su agricultura orgánica sostenible y educación rural."
            ),
            historyNote = LocalizedText(
                en = "Accredited by the Agricultural Training Institute (ATI) as a cornerstone of Dumingag's Organic Agriculture Capital initiative.",
                ceb = "Giila sa Agricultural Training Institute (ATI) isip sumbanan sa organikong panguma sa Dumingag.",
                tl = "Kinilala ng Agricultural Training Institute (ATI) bilang huwarang sentro ng organikong agrikultura sa Dumingag.",
                es = "Acreditado por el Instituto de Capacitación Agrícola (ATI) como pilar de la capital orgánica de Dumingag."
            ),
            barangay = "Libertad",
            category = AttractionCategory.ORGANIC_FARM,
            latitude = 8.1460,
            longitude = 123.3458,
            mapX = 0.49f,
            mapY = 0.69f,
            imageRes = R.drawable.img_organic_village,
            openHours = "07:00 AM – 05:00 PM",
            entryFee = LocalizedText("Coordinate with Municipal Tourism", "Pakig-alayon sa Turismo", "Makipag-ugnayan sa Turismo", "Coordinar con Turismo Municipal"),
            bestTime = LocalizedText("Morning 7:00–10:30 AM", "Buntag 7:00–10:30 AM", "Umaga 7:00–10:30 AM", "Mañana 7:00–10:30 AM"),
            transitHint = LocalizedText(
                en = "12 mins south of Poblacion Market via tricycle or habal-habal to Purok 6, Brgy. Libertad.",
                ceb = "12 minutos habagatan sa Merkado sakay sa traysikol paingon sa Brgy. Libertad.",
                tl = "12 minuto sa timog mula Poblacion sakay ng traysikel patungong Brgy. Libertad.",
                es = "12 min al sur de Poblacion en triciclo hasta Purok 6, Brgy. Libertad."
            ),
            baseRating = 4.9
        ),
        Attraction(
            id = "danlugan_falls",
            mapNodeId = "node_danlugan",
            name = LocalizedText(
                en = "Danlugan Highland Cascades",
                ceb = "Busay sa Danlugan",
                tl = "Talon ng Danlugan",
                es = "Cascadas de Danlugan"
            ),
            subtitle = LocalizedText(
                en = "Rainforest Plunge Pools & Fern Gorge",
                ceb = "Bugnaw nga Busay ug Lasang",
                tl = "Malamig na Talon sa Kagubatan",
                es = "Pozas Esmeralda y Cañón Tropical"
            ),
            description = LocalizedText(
                en = "Tucked beneath lush Mindanao ferns and mossy boulders, Danlugan Falls rewards trekkers with cool mountain water, terraced cascades, and shady picnic rock ledges.",
                ceb = "Nahimutang sa mabugnaw nga lasang, ang Busay sa Danlugan adunay tin-aw nga tubig ug nindot nga kaligoanan.",
                tl = "Nakatago sa luntiang kagubatan, nag-aalok ang Talon ng Danlugan ng malamig na tubig at malilim na piknikan.",
                es = "Escondida entre helechos y rocas musgosas, ofrece aguas cristalinas de montaña y pozas refrescantes."
            ),
            historyNote = LocalizedText(
                en = "Named after the Subanen word for clear mountain stream; protected by local barangay watershed stewards.",
                ceb = "Gipanalipdan sa mga lumulupyo sa Barangay Danlugan isip tinubdan sa limpyo nga tubig.",
                tl = "Pinangangalagaan ng barangay bilang mahalagang watershed at likas na yaman.",
                es = "Protegida por la comunidad local de Danlugan como santuario natural de agua pura."
            ),
            barangay = "Danlugan",
            category = AttractionCategory.NATURE,
            latitude = 8.1910,
            longitude = 123.3185,
            mapX = 0.25f,
            mapY = 0.26f,
            imageRes = R.drawable.img_waterfall_falls,
            openHours = "06:30 AM – 04:30 PM",
            entryFee = LocalizedText("PHP 40 environmental fee", "PHP 40 bayad sa kinaiyahan", "PHP 40 environmental fee", "PHP 40 tarifa ecológica"),
            bestTime = LocalizedText("Dry mornings Dec–May", "Ting-init Disyembre–Mayo", "Umaga mula Disyembre–Mayo", "Mañanas secas dic–may"),
            transitHint = LocalizedText(
                en = "25 mins by habal-habal northwest via San Pablo–Danlugan Trail.",
                ceb = "25 minutos sakay sa habal-habal agi sa San Pablo paingon Danlugan.",
                tl = "25 minuto sa habal-habal dadaan sa San Pablo papuntang Danlugan.",
                es = "25 min en mototaxi al noroeste por la ruta San Pablo–Danlugan."
            ),
            baseRating = 4.7
        ),
        Attraction(
            id = "mt_timolan_trail",
            mapNodeId = "node_timolan",
            name = LocalizedText(
                en = "Mt. Timolan Ridge Viewpoint",
                ceb = "Bukid sa Timolan Viewdeck",
                tl = "Mt. Timolan Ridge Viewpoint",
                es = "Mirador del Monte Timolan"
            ),
            subtitle = LocalizedText(
                en = "Protected Landscape & Sea-of-Clouds Trek",
                ceb = "Gipanalipdang Lasang ug Pangpang",
                tl = "Protektadong Bundok at Dagat ng Ulap",
                es = "Paisaje Protegido y Mar de Nubes"
            ),
            description = LocalizedText(
                en = "A serene upland trail bordering the Mt. Timolan Protected Landscape. On crisp mornings, mist blankets the Zamboanga del Sur valley below while hornbills echo in the canopy.",
                ceb = "Hataas nga bukid diin makita ang dagat sa panganod sa kabuntagon ug ang tibuok walog sa Dumingag.",
                tl = "Mataas na tanawin sa hangganan ng Mt. Timolan kung saan matatanaw ang dagat ng ulap sa umaga.",
                es = "Sendero de altura junto al Paisaje Protegido del Monte Timolan con vistas panorámicas entre nubes matinales."
            ),
            historyNote = LocalizedText(
                en = "Part of the Mount Timolan Protected Landscape proclaimed to preserve Mindanao highland biodiversity and springs.",
                ceb = "Kabahin sa gipanalipdang lasang sa Zamboanga del Sur alang sa kalasangan ug mga langgam.",
                tl = "Bahagi ng Mount Timolan Protected Landscape para sa konserbasyon ng kalikasan sa Mindanao.",
                es = "Parte del área natural protegida declarada para conservar la biodiversidad montañosa de Mindanao."
            ),
            barangay = "Upper Timolan Border",
            category = AttractionCategory.NATURE,
            latitude = 8.1360,
            longitude = 123.3110,
            mapX = 0.18f,
            mapY = 0.78f,
            imageRes = R.drawable.img_dumingag_hero,
            openHours = "05:00 AM – 04:00 PM (Guide recommended)",
            entryFee = LocalizedText("PHP 50 + local guide", "PHP 50 + giya sa bukid", "PHP 50 + lokal na gabay", "PHP 50 + guía local"),
            bestTime = LocalizedText("Sunrise 5:30–7:30 AM", "Pagsubang sa adlaw 5:30–7:30 AM", "Pagsikat ng araw 5:30–7:30 AM", "Amanecer 5:30–7:30 AM"),
            transitHint = LocalizedText(
                en = "35 mins habal-habal southwest to Upper Landing jump-off.",
                ceb = "35 minutos habal-habal paingon sa Upper Landing jump-off.",
                tl = "35 minuto habal-habal patungong Upper Landing jump-off.",
                es = "35 min en mototaxi hacia el suroeste hasta Upper Landing."
            ),
            baseRating = 4.9
        ),
        Attraction(
            id = "subanen_heritage",
            mapNodeId = "node_cultural_hub",
            name = LocalizedText(
                en = "Subanen Cultural & Weaving Center",
                ceb = "Sentro sa Kultura sa Subanen",
                tl = "Sentro ng Kulturang Subanen",
                es = "Centro Cultural Subanen"
            ),
            subtitle = LocalizedText(
                en = "Indigenous Craft, Brassware & Oral History",
                ceb = "Tradisyonal nga Paghabol ug Kabilin",
                tl = "Katutubong Sining at Kasaysayan",
                es = "Artesanía Indígena y Memoria Viva"
            ),
            description = LocalizedText(
                en = "Celebrate the indigenous Subanen roots of Dumingag. Explore traditional woven textiles, bamboo percussion instruments, and community storytelling exhibits.",
                ceb = "Ilha ang lumadnong kultura sa Subanen pinaagi sa tradisyonal nga panapton, musika, ug kasaysayan.",
                tl = "Kilalanin ang katutubong kulturang Subanen sa pamamagitan ng tradisyonal na habi, musika, at sining.",
                es = "Conozca las raíces indígenas Subanen con textiles tradicionales, música de bambú y relatos comunitarios."
            ),
            historyNote = LocalizedText(
                en = "Dumingag derives from the Subanen term for peaceful river bend; the center preserves ancestral crafts and beadwork.",
                ceb = "Ang ngalang Dumingag gikan sa karaang pinulongang Subanen; gitipigan dinhi ang mga kabilin.",
                tl = "Nagmula ang pangalang Dumingag sa wikang Subanen; pinapahalagahan dito ang kanilang pamana.",
                es = "Preserva el legado ancestral Subanen que dio origen al nombre e identidad de Dumingag."
            ),
            barangay = "San Pedro / Poblacion",
            category = AttractionCategory.CULTURAL,
            latitude = 8.1678,
            longitude = 123.3412,
            mapX = 0.44f,
            mapY = 0.45f,
            imageRes = R.drawable.img_heritage_plaza,
            openHours = "08:00 AM – 05:00 PM (Mon–Sat)",
            entryFee = LocalizedText("PHP 20 heritage donation", "PHP 20 donasyon", "PHP 20 donasyon", "PHP 20 donación"),
            bestTime = LocalizedText("Mid-morning 9:00–11:30 AM", "Buntag 9:00–11:30 AM", "Umaga 9:00–11:30 AM", "Media mañana 9:00–11:30"),
            transitHint = LocalizedText(
                en = "5 mins by tricycle north of Municipal Hall.",
                ceb = "5 minutos sa traysikol gikan sa Munisipyo.",
                tl = "5 minuto sa traysikel mula sa Munisipyo.",
                es = "5 min en triciclo al norte del Ayuntamiento."
            ),
            baseRating = 4.6
        ),
        Attraction(
            id = "maragang_terraces",
            mapNodeId = "node_maragang",
            name = LocalizedText(
                en = "Maragang Valley & Terrace Deck",
                ceb = "Maragang Valley Viewdeck",
                tl = "Maragang Valley Viewdeck",
                es = "Mirador del Valle Maragang"
            ),
            subtitle = LocalizedText(
                en = "Panoramic Rice Terraces & Highland Coffee",
                ceb = "Panoramikong Humayan ug Kape",
                tl = "Tanawin ng Hagdan-hagdang Palayan",
                es = "Terrazas de Arroz y Café de Altura"
            ),
            description = LocalizedText(
                en = "Overlooking undulating emerald rice terraces and upland coffee groves, Maragang Deck is the quintessential golden-hour photography stop in Dumingag.",
                ceb = "Nindot nga talan-awon sa berdeng humayan ug kabukiran; perpekto para sa hulagway ug kape sa hapon.",
                tl = "Matatanaw ang luntiang palayan at taniman ng kape; paboritong hintuan para sa kape at larawan.",
                es = "Mirador panorámico sobre terrazas verdes de arroz y cafetales de montaña al atardecer."
            ),
            historyNote = LocalizedText(
                en = "Developed by highland farmers along the scenic ridge connecting Dumingag's agricultural barangays.",
                ceb = "Gipanindot sa mga mag-uuma sa Maragang isip pahuwayanan sa mga byahero.",
                tl = "Itinaguyod ng mga magsasaka ng Maragang bilang pahingahan ng mga manlalakbay.",
                es = "Creado por agricultores locales de Maragang a lo largo de la ruta panorámica."
            ),
            barangay = "Maragang",
            category = AttractionCategory.LANDMARK,
            latitude = 8.1510,
            longitude = 123.3690,
            mapX = 0.79f,
            mapY = 0.66f,
            imageRes = R.drawable.img_dumingag_hero,
            openHours = "06:00 AM – 06:30 PM",
            entryFee = LocalizedText("PHP 20 viewdeck fee", "PHP 20 bayad sa viewdeck", "PHP 20 viewdeck fee", "PHP 20 acceso"),
            bestTime = LocalizedText("4:00–5:45 PM Sunset", "4:00–5:45 PM Pagsalop", "4:00–5:45 PM Paglubog", "16:00–17:45 Atardecer"),
            transitHint = LocalizedText(
                en = "18 mins via tricycle or van southeast on Dumingag–Mahayag Rd.",
                ceb = "18 minutos sakay sa traysikol o van paingon sa Maragang.",
                tl = "18 minuto sa traysikel o van patungong Maragang.",
                es = "18 min en triciclo o van hacia el sureste."
            ),
            baseRating = 4.8
        ),
        Attraction(
            id = "pasalubong_market",
            mapNodeId = "node_market",
            name = LocalizedText(
                en = "Dumingag Gymnasium & Night Market Hub",
                ceb = "Dumingag Gymnasium ug Night Market",
                tl = "Dumingag Gymnasium at Night Market",
                es = "Gimnasio y Mercado Nocturno de Dumingag"
            ),
            subtitle = LocalizedText(
                en = "Heirloom Red Rice, Cacao Tablea & Civic Arena",
                ceb = "Organikong Bugas, Sikwate ug Merkado",
                tl = "Organikong Bigas, Tablea at Pamilihan",
                es = "Arroz Rojo, Cacao Tablea y Plaza Cívica"
            ),
            description = LocalizedText(
                en = "Explore Dumingag Gymnasium, the vibrant Night Market featured on dumingag.com, and local cooperative stalls offering heirloom organic rice, stone-ground cacao tablea, and native kakanin.",
                ceb = "Bisitaha ang Dumingag Gymnasium ug bibo nga Night Market nga makita sa dumingag.com alang sa organikong bugas, tablea, ug kakanin.",
                tl = "Bisitahin ang Dumingag Gymnasium at Night Market para sa organikong bigas, tablea, pulot, at mga katutubong kakanin.",
                es = "Visite el Gimnasio y el Mercado Nocturno destacado en dumingag.com para probar arroz orgánico, cacao tablea y dulces locales."
            ),
            historyNote = LocalizedText(
                en = "Serves as the civic gathering and trading center for 44 barangays of Dumingag, lively during festivals and weekend market nights.",
                ceb = "Sentro sa komersyo ug panagtapok sa 44 ka barangay sa Dumingag, ilabi na sa Sabado ug mga pista.",
                tl = "Sentro ng kalakalan at pagtitipon ng 44 na barangay ng Dumingag, lalo na tuwing Sabado at pista.",
                es = "Centro comercial y cívico de los 44 barangays de Dumingag, muy animado los fines de semana."
            ),
            barangay = "Poblacion Market District",
            category = AttractionCategory.LANDMARK,
            latitude = 8.1632,
            longitude = 123.3502,
            mapX = 0.56f,
            mapY = 0.56f,
            imageRes = R.drawable.img_organic_village,
            openHours = "05:00 AM – 09:00 PM",
            entryFee = LocalizedText("Free entry", "Libre ang pagsulod", "Libreng pagpasok", "Entrada gratuita"),
            bestTime = LocalizedText("Evening Night Market & Sat AM", "Gabii sa Night Market ug Sabado", "Gabi ng Night Market at Sabado", "Mercado nocturno y sábados"),
            transitHint = LocalizedText(
                en = "2 mins walk from Integrated Bus Terminal.",
                ceb = "2 minutos lakaw gikan sa Terminal.",
                tl = "2 minutong lakad mula sa Terminal.",
                es = "A 2 minutos a pie de la Terminal."
            ),
            baseRating = 4.7
        ),
        Attraction(
            id = "lipawan_cave",
            mapNodeId = "node_lipawan",
            name = LocalizedText(
                en = "Lipawan Cave Conservation Site",
                ceb = "Langob sa Lipawan",
                tl = "Kweba ng Lipawan",
                es = "Cueva de Lipawan"
            ),
            subtitle = LocalizedText(
                en = "Protected Limestone Cavern (dumingag.com)",
                ceb = "Gipanalipdang Langob sa Brgy. Lipawan",
                tl = "Protektadong Kweba sa Brgy. Lipawan",
                es = "Caverna Protegida de Ecoturismo"
            ),
            description = LocalizedText(
                en = "A municipally protected natural cave system in Barangay Lipawan highlighted on dumingag.com. Ideal for guided educational eco-adventures following LGU conservation ordinances.",
                ceb = "Gipanalipdan nga natural nga langob sa Barangay Lipawan nga gipakita sa dumingag.com; pangayo og giya sa Turismo sa dili pa mosulod.",
                tl = "Pinangangalagaang kweba sa Barangay Lipawan na tampok sa dumingag.com para sa edukasyonal at gabay na ekoturismo.",
                es = "Sistema de cuevas naturales protegido por ordenanza municipal en Brgy. Lipawan, ideal para recorridos guiados."
            ),
            historyNote = LocalizedText(
                en = "Protected under Dumingag municipal ecotourism and environmental conservation ordinances; visitors register with the Municipal Tourism Office.",
                ceb = "Gipanalipdan ubos sa ordinansa sa kinaiyahan sa Dumingag; magparehistro sa Municipal Tourism Office.",
                tl = "Pinoprotektahan ng lokal na ordinansa; kailangang magparehistro sa Municipal Tourism Office bago bumisita.",
                es = "Protegida bajo ordenanzas locales de conservación; requiere registro previo en Turismo Municipal."
            ),
            barangay = "Lipawan",
            category = AttractionCategory.NATURE,
            latitude = 8.1718,
            longitude = 123.3453,
            mapX = 0.48f,
            mapY = 0.42f,
            imageRes = R.drawable.img_waterfall_falls,
            openHours = "07:00 AM – 04:00 PM (Tourism Coordination)",
            entryFee = LocalizedText("LGU guide permit required", "Kinahanglan og permit sa Turismo", "Kailangan ng permit sa Turismo", "Permiso municipal requerido"),
            bestTime = LocalizedText("Dry morning 8:00–11:30 AM", "Buntag sa ting-init", "Maagang umaga sa tag-init", "Mañanas secas 8:00–11:30"),
            transitHint = LocalizedText(
                en = "10 mins north of Poblacion Plaza via tricycle or habal-habal to Brgy. Lipawan.",
                ceb = "10 minutos amihanan sa Plaza sakay sa traysikol paingon sa Brgy. Lipawan.",
                tl = "10 minuto sa hilaga ng Plaza patungong Brgy. Lipawan.",
                es = "10 min al norte de Poblacion Plaza hacia Brgy. Lipawan."
            ),
            baseRating = 4.7
        )
    )

    val mapNodes: List<OfflineMapNode> = listOf(
        OfflineMapNode("node_poblacion", LocalizedText("Poblacion Town Plaza", "Plaza sa Poblacion", "Plaza ng Poblacion", "Plaza Poblacion"), 0.50f, 0.52f, 8.1650, 123.3465, true),
        OfflineMapNode("node_market", LocalizedText("Bus Terminal & Gymnasium", "Terminal ug Gymnasium", "Terminal at Gymnasium", "Terminal y Gimnasio"), 0.56f, 0.56f, 8.1632, 123.3502, true),
        OfflineMapNode("node_cultural_hub", LocalizedText("Subanen Cultural Center", "Sentro sa Subanen", "Sentro ng Subanen", "Centro Subanen"), 0.44f, 0.45f, 8.1678, 123.3412, true),
        OfflineMapNode("node_lipawan", LocalizedText("Brgy. Lipawan Cave Hub", "Langob sa Lipawan", "Kweba ng Lipawan", "Cueva de Lipawan"), 0.48f, 0.42f, 8.1718, 123.3453, true),
        OfflineMapNode("node_san_pablo", LocalizedText("San Pablo Junction", "Krusada sa San Pablo", "Sangandaan ng San Pablo", "Cruce San Pablo"), 0.38f, 0.37f, 8.1765, 123.3320, false),
        OfflineMapNode("node_danlugan", LocalizedText("Danlugan Falls Trailhead", "Danlugan Falls", "Talon ng Danlugan", "Cascadas Danlugan"), 0.25f, 0.26f, 8.1910, 123.3185, true),
        OfflineMapNode("node_libertad", LocalizedText("Brgy. Libertad (Paglinawan Eco-Farm)", "Paglinawan Eco-Farm Libertad", "Paglinawan Eco-Farm Libertad", "Granja Paglinawan Libertad"), 0.49f, 0.69f, 8.1460, 123.3458, true),
        OfflineMapNode("node_lower_landing", LocalizedText("Lower Landing Road", "Lower Landing", "Lower Landing", "Lower Landing"), 0.68f, 0.41f, 8.1742, 123.3620, false),
        OfflineMapNode("node_tagun", LocalizedText("Tagun River Bridge", "Taytayan sa Tagun", "Tulay ng Tagun", "Puente Tagun"), 0.63f, 0.58f, 8.1585, 123.3575, false),
        OfflineMapNode("node_maragang", LocalizedText("Maragang Terrace Deck", "Maragang Viewdeck", "Maragang Viewdeck", "Mirador Maragang"), 0.79f, 0.66f, 8.1510, 123.3690, true),
        OfflineMapNode("node_upper_landing", LocalizedText("Upper Landing Jump-Off", "Upper Landing", "Upper Landing", "Cruce Upper Landing"), 0.34f, 0.66f, 8.1495, 123.3265, false),
        OfflineMapNode("node_timolan", LocalizedText("Mt. Timolan Viewpoint", "Mt. Timolan Viewdeck", "Mt. Timolan Viewpoint", "Mirador Mt. Timolan"), 0.18f, 0.78f, 8.1360, 123.3110, true)
    )

    val mapEdges: List<OfflineMapEdge> = listOf(
        OfflineMapEdge("node_poblacion", "node_market", 0.5, "Rizal Avenue", "Paved Town Street"),
        OfflineMapEdge("node_poblacion", "node_cultural_hub", 0.7, "Magsaysay Street", "Paved Town Street"),
        OfflineMapEdge("node_poblacion", "node_lipawan", 0.8, "Lipawan Barangay Rd", "Paved Barangay Road"),
        OfflineMapEdge("node_lipawan", "node_san_pablo", 1.5, "Lipawan–San Pablo Link", "Paved Barangay Road"),
        OfflineMapEdge("node_cultural_hub", "node_san_pablo", 1.6, "Dumingag–Dipolog Provincial Rd", "Paved Highway"),
        OfflineMapEdge("node_san_pablo", "node_danlugan", 2.8, "Danlugan Eco-Trail Rd", "Farm-to-Market Road"),
        OfflineMapEdge("node_poblacion", "node_libertad", 2.2, "Libertad Eco-Tourism Rd", "Paved Barangay Road"),
        OfflineMapEdge("node_libertad", "node_upper_landing", 2.1, "Libertad–Upper Landing Link", "Concrete Upland Road"),
        OfflineMapEdge("node_poblacion", "node_lower_landing", 2.1, "Lower Landing Organic Rd", "Paved Barangay Road"),
        OfflineMapEdge("node_market", "node_tagun", 1.2, "Dumingag–Molave Highway", "National Highway"),
        OfflineMapEdge("node_tagun", "node_lower_landing", 1.9, "Riverside Link Rd", "Paved Barangay Road"),
        OfflineMapEdge("node_tagun", "node_maragang", 2.4, "Maragang Ridge Rd", "Concrete Upland Road"),
        OfflineMapEdge("node_poblacion", "node_upper_landing", 2.5, "Upper Landing Rd", "Concrete Road"),
        OfflineMapEdge("node_upper_landing", "node_timolan", 3.2, "Timolan Sanctuary Trail", "Highland Ridge Trail"),
        OfflineMapEdge("node_upper_landing", "node_cultural_hub", 2.2, "West Poblacion Bypass", "Gravel Link Road")
    )

    val transitRoutes: List<TransitRoute> = listOf(
        TransitRoute(
            id = "bus_pagadian_dumingag",
            title = LocalizedText(
                en = "Pagadian City ↔ Dumingag Terminal",
                ceb = "Pagadian City ↔ Dumingag Terminal",
                tl = "Pagadian City ↔ Dumingag Terminal",
                es = "Pagadian ↔ Terminal de Dumingag"
            ),
            operatorName = "Rural Transit / UV Express Vans",
            vehicleType = "Aircon Bus & Commuter Van",
            scheduleSummary = LocalizedText(
                en = "Every 45 mins from 05:00 AM to 05:30 PM daily",
                ceb = "Matag 45 minutos gikan 5:00 AM hangtod 5:30 PM",
                tl = "Tuwing 45 minuto mula 5:00 AM hanggang 5:30 PM",
                es = "Cada 45 min de 05:00 a 17:30 diariamente"
            ),
            fareRangePhp = "₱95 – ₱130",
            durationText = "1 hr 15 mins",
            terminalNote = LocalizedText(
                en = "Via Molave–Mahayag Highway; stops directly at Poblacion Market Terminal.",
                ceb = "Agi sa Molave–Mahayag Highway; hunong mismo sa Merkado Terminal.",
                tl = "Dadaan sa Molave–Mahayag Highway; baba sa Poblacion Terminal.",
                es = "Por la carretera Molave–Mahayag hasta la Terminal Poblacion."
            )
        ),
        TransitRoute(
            id = "bus_dipolog_dumingag",
            title = LocalizedText(
                en = "Dipolog City ↔ Dumingag (via Sergio Osmeña)",
                ceb = "Dipolog City ↔ Dumingag (agi sa Osmeña)",
                tl = "Dipolog City ↔ Dumingag (via Osmeña)",
                es = "Dipolog ↔ Dumingag (vía Osmeña)"
            ),
            operatorName = "Bachelor / Dipolog–Dumingag Vans",
            vehicleType = "Mountain Van & Minibus",
            scheduleSummary = LocalizedText(
                en = "Hourly departures 05:30 AM – 04:00 PM",
                ceb = "Matag oras 5:30 AM – 4:00 PM",
                tl = "Oras-oras mula 5:30 AM – 4:00 PM",
                es = "Salidas cada hora de 05:30 a 16:00"
            ),
            fareRangePhp = "₱120 – ₱160",
            durationText = "1 hr 40 mins",
            terminalNote = LocalizedText(
                en = "Scenic mountain pass route; sit on left side for ridge views.",
                ceb = "Nindot nga agianan sa kabukiran; lingkod sa wala nga kilid.",
                tl = "Magandang tanawin sa kabundukan; umupo sa kaliwa.",
                es = "Ruta escénica de montaña; vistas espectaculares."
            )
        ),
        TransitRoute(
            id = "tricycle_poblacion",
            title = LocalizedText(
                en = "Poblacion Tricycle & Bao-Bao Loop",
                ceb = "Traysikol ug Bao-Bao sa Poblacion",
                tl = "Traysikel at Bao-Bao sa Poblacion",
                es = "Triciclo Local Poblacion"
            ),
            operatorName = "Dumingag TODA",
            vehicleType = "Tricycle / Electric Bao-Bao",
            scheduleSummary = LocalizedText(
                en = "Continuous 05:00 AM – 09:00 PM around Poblacion & Lower Landing",
                ceb = "Kanunay 5:00 AM – 9:00 PM sulod sa Poblacion ug Lower Landing",
                tl = "Tuloy-tuloy 5:00 AM – 9:00 PM sa Poblacion at Lower Landing",
                es = "Servicio continuo de 05:00 a 21:00"
            ),
            fareRangePhp = "₱15 – ₱35",
            durationText = "5 – 12 mins",
            terminalNote = LocalizedText(
                en = "Standard fare ₱15 within town center; ₱30–35 to Organic Eco-Village.",
                ceb = "₱15 sulod sa lungsod; ₱30–35 paingon sa Organic Eco-Village.",
                tl = "₱15 sa bayan; ₱30–35 papuntang Organic Eco-Village.",
                es = "Tarifa estándar ₱15 en el centro; ₱30–35 a la Aldea Orgánica."
            )
        ),
        TransitRoute(
            id = "habal_upland",
            title = LocalizedText(
                en = "Upland Habal-Habal (Danlugan & Mt. Timolan)",
                ceb = "Habal-Habal paingon Danlugan ug Timolan",
                tl = "Habal-Habal (Danlugan at Mt. Timolan)",
                es = "Mototaxi de Montaña (Danlugan / Timolan)"
            ),
            operatorName = "Barangay Habal-Habal Riders Assoc.",
            vehicleType = "Mountain Motorcycle Taxi",
            scheduleSummary = LocalizedText(
                en = "On-demand at Public Market & San Pablo Junction (05:30 AM – 04:30 PM)",
                ceb = "Naa sa Merkado ug San Pablo Junction (5:30 AM – 4:30 PM)",
                tl = "May pila sa Pamilihan at San Pablo Junction (5:30 AM – 4:30 PM)",
                es = "Disponible bajo demanda en Mercado y San Pablo (05:30–16:30)"
            ),
            fareRangePhp = "₱80 – ₱150 / way",
            durationText = "20 – 35 mins",
            terminalNote = LocalizedText(
                en = "Always ask for a helmet and agree on round-trip waiting rate before departure.",
                ceb = "Pangayo kanunay og helmet ug saboti ang plete sa balik daan.",
                tl = "Magsuot ng helmet at itanong ang pamasahe bago umalis.",
                es = "Solicite casco y acuerde la tarifa de ida y vuelta antes de salir."
            )
        )
    )

    val defaultTransitMethods: List<TransitMethod> = listOf(
        TransitMethod(
            id = "tricycle_poblacion",
            name = "Poblacion Tricycle & Electric Bao-Bao",
            modeCategory = TransitModeCategory.TRICYCLE.code,
            operatorName = "Dumingag TODA",
            routeSummary = "Poblacion town center, Public Market, San Isidro Parish, Gymnasium & Lower Landing",
            scheduleText = "Continuous 05:00 AM – 09:00 PM daily",
            fareRangePhp = "₱15 – ₱35",
            durationText = "5 – 12 mins",
            terminalLocation = "Dumingag Public Market & Municipal Plaza",
            travelTip = "Standard fare ₱15 within town center; ₱30–₱35 per passenger toward Lower Landing.",
            serviceStatus = TransitServiceStatus.OPERATIONAL.code,
            visibility = "public"
        ),
        TransitMethod(
            id = "habal_upland",
            name = "Upland Habal-Habal (Mountain Motorcycle)",
            modeCategory = TransitModeCategory.HABAL_HABAL.code,
            operatorName = "Barangay Habal-Habal Riders Assoc.",
            routeSummary = "Danlugan Falls, Mt. Timolan Ridge, Lipawan Cave & Paglinawan Organic Eco-Farm (Brgy. Libertad)",
            scheduleText = "On-demand 05:30 AM – 04:30 PM daily",
            fareRangePhp = "₱40 – ₱150 / way",
            durationText = "15 – 35 mins",
            terminalLocation = "Public Market Stand & San Pablo Junction",
            travelTip = "Always wear a helmet and confirm round-trip waiting rates with the rider before departing.",
            serviceStatus = TransitServiceStatus.OPERATIONAL.code,
            visibility = "public"
        ),
        TransitMethod(
            id = "bus_pagadian_dumingag",
            name = "Pagadian City ↔ Dumingag Commuter Bus & Van",
            modeCategory = TransitModeCategory.BUS_VAN.code,
            operatorName = "Rural Transit / UV Express Vans",
            routeSummary = "Pagadian Integrated Terminal ↔ Molave–Mahayag Highway ↔ Dumingag Poblacion",
            scheduleText = "Every 45 mins from 05:00 AM to 05:30 PM daily",
            fareRangePhp = "₱95 – ₱130",
            durationText = "1 hr 15 mins",
            terminalLocation = "Dumingag Poblacion Public Terminal",
            travelTip = "Direct highway link via Molave & Mahayag; final Pagadian-bound van departs around 5:30 PM.",
            serviceStatus = TransitServiceStatus.OPERATIONAL.code,
            visibility = "public"
        ),
        TransitMethod(
            id = "bus_dipolog_dumingag",
            name = "Dipolog City ↔ Dumingag Mountain Van",
            modeCategory = TransitModeCategory.BUS_VAN.code,
            operatorName = "Dipolog–Dumingag Commuter Vans",
            routeSummary = "Dipolog City ↔ Sergio Osmeña Highland Road ↔ Dumingag Poblacion",
            scheduleText = "Hourly departures 05:30 AM – 04:00 PM daily",
            fareRangePhp = "₱120 – ₱160",
            durationText = "1 hr 40 mins",
            terminalLocation = "Dumingag Poblacion Public Terminal",
            travelTip = "Scenic mountain pass through Sergio Osmeña; bring a light jacket for early morning trips.",
            serviceStatus = TransitServiceStatus.OPERATIONAL.code,
            visibility = "public"
        ),
        TransitMethod(
            id = "multicab_barangay",
            name = "Barangay Multicab & Market-Day Jeepney",
            modeCategory = TransitModeCategory.JEEPNEY_MULTICAB.code,
            operatorName = "Dumingag Rural Jeepney & Multicab Coop.",
            routeSummary = "Poblacion Public Market ↔ Libertad, San Pablo, Lipawan & Upper Landing Barangays",
            scheduleText = "05:00 AM – 04:00 PM (Frequent on Market Days)",
            fareRangePhp = "₱25 – ₱60",
            durationText = "15 – 40 mins",
            terminalLocation = "Dumingag Public Market Loading Bay",
            travelTip = "Best budget ride to Brgy. Libertad (Paglinawan Organic Eco-Farm) and rural barangays.",
            serviceStatus = TransitServiceStatus.OPERATIONAL.code,
            visibility = "public"
        )
    )

    val emergencyContacts: List<EmergencyContact> = listOf(
        EmergencyContact(
            id = "nat_911",
            title = LocalizedText(
                en = "National Emergency Hotline",
                ceb = "Nasyonal nga Emerhensya",
                tl = "Pambansang Emerhensya",
                es = "Emergencia Nacional"
            ),
            department = "Philippine Emergency 911 (Police, Fire & Medical)",
            phoneNumber = "911",
            availability = "24/7 Dispatch",
            priorityLevel = "CRITICAL"
        ),
        EmergencyContact(
            id = "mayor_hotline",
            title = LocalizedText(
                en = "Office of the Municipal Mayor & LGU Hotline",
                ceb = "Opisina sa Mayor / LGU Dumingag",
                tl = "Tanggapan ng Alkalde / LGU Dumingag",
                es = "Alcaldía Municipal de Dumingag"
            ),
            department = "Municipal Hall, Poblacion, Dumingag (dumingag.com)",
            phoneNumber = "0948-212-1024",
            availability = "Official LGU Mobile Hotline",
            priorityLevel = "HIGH",
            email = "mmo.lgudumingag@gmail.com",
            websiteUrl = "https://dumingag.com"
        ),
        EmergencyContact(
            id = "lgu_admin_eoc",
            title = LocalizedText(
                en = "LGU Dumingag Municipal Hall & MDRRMC EOC",
                ceb = "Munisipyo ug MDRRMC Emergency Center",
                tl = "Munisipyo at MDRRMC Emergency Center",
                es = "Centro de Operaciones y Emergencias (MDRRMC)"
            ),
            department = "Disaster Response, PNP & BFP Coordination Hub",
            phoneNumber = "(062) 211-2623",
            availability = "Municipal Hall Trunkline",
            priorityLevel = "HIGH",
            email = "info.lgudumingag@gmail.com",
            websiteUrl = "https://dumingag.com"
        ),
        EmergencyContact(
            id = "rhu_health",
            title = LocalizedText(
                en = "Dumingag Rural Health Unit (RHU)",
                ceb = "Dumingag Health Center / RHU",
                tl = "Dumingag Rural Health Unit",
                es = "Centro de Salud Rural Dumingag"
            ),
            department = "Poblacion Municipal Health & First Aid (via LGU EOC)",
            phoneNumber = "(062) 211-2623",
            availability = "Emergency Triage • Dial 911 for urgent EMS",
            priorityLevel = "MEDICAL",
            email = "info.lgudumingag@gmail.com",
            websiteUrl = "https://dumingag.com"
        ),
        EmergencyContact(
            id = "tourism_desk",
            title = LocalizedText(
                en = "Dumingag Municipal Tourism Office",
                ceb = "Opisina sa Turismo sa Dumingag",
                tl = "Tanggapan ng Turismo ng Dumingag",
                es = "Oficina de Turismo Municipal"
            ),
            department = "Ecotourism Permits, Lipawan Cave & Guide Coordination",
            phoneNumber = "0948-212-1024",
            availability = "Mon–Fri 8:00 AM – 5:00 PM • dumingag.com",
            priorityLevel = "ASSISTANCE",
            email = "info.lgudumingag@gmail.com",
            websiteUrl = "https://dumingag.com"
        )
    )

    val starterDocuments: List<SavedDocument> = listOf(
        SavedDocument(
            id = "doc_offline_pack",
            title = "Dumingag Offline Field & Trail Guide",
            category = "Trail Map & Permits",
            summary = "Complete offline trail notes, Mt. Timolan guide registration rules, and Poblacion landmarks.",
            contentBody = """
                DUMINGAG OFFLINE FIELD GUIDE (Zamboanga del Sur)
                • Town Center Coordinates: 8.1650° N, 123.3465° E (Elevation ~145m)
                • Mt. Timolan Trekking: Register at Upper Landing Barangay Hall before 6:00 AM. Mandatory local guide for ridge trails.
                • Danlugan Cascades: Wear gripped water shoes; avoid swimming immediately after heavy highland rain.
                • Organic Village Etiquette: Respect terraced seed plots; plastic bottles are discouraged inside Gintong Ani Demo Farm.
            """.trimIndent(),
            savedAtMillis = 1763400000000L,
            isBundled = true
        ),
        SavedDocument(
            id = "doc_transit_cheat_sheet",
            title = "Dumingag Fare Matrix & Terminal Card",
            category = "Transit Document",
            summary = "Offline reference for Pagadian/Dipolog buses, Poblacion tricycles, and habal-habal rates.",
            contentBody = """
                OFFICIAL TRANSIT & FARE QUICK REFERENCE
                1. Pagadian -> Dumingag Bus/Van: PHP 95–130 (Last trip 5:30 PM)
                2. Dipolog -> Dumingag Van: PHP 120–160 (Last trip 4:00 PM)
                3. Poblacion Tricycle: PHP 15 base fare; PHP 30–35 to Lower Landing Organic Village
                4. Upland Habal-Habal: PHP 80–150 one-way to Danlugan Falls or Upper Landing
            """.trimIndent(),
            savedAtMillis = 1763400000000L,
            isBundled = true
        ),
        SavedDocument(
            id = "doc_safety_protocol",
            title = "Offline Emergency & Weather Protocol",
            category = "Traveler Safety",
            summary = "First-aid checkpoints, MDRRMO radio zones, and rain-advisory fallback routes.",
            contentBody = """
                OFFLINE SAFETY CHECKLIST
                • Mobile signal weakens beyond San Pablo Junction and Upper Timolan Ridge.
                • Always screenshot or save your itinerary and emergency contacts offline before heading to Danlugan or Timolan.
                • Primary Emergency Hub: Dumingag Municipal Hall / RHU along Rizal Avenue (Poblacion).
            """.trimIndent(),
            savedAtMillis = 1763400000000L,
            isBundled = true
        )
    )
}

object OfflineRouteEngine {

    fun findNearestNode(lat: Double, lon: Double): OfflineMapNode {
        return DumingagSeedData.mapNodes.minByOrNull { node ->
            haversineKm(lat, lon, node.latitude, node.longitude)
        } ?: DumingagSeedData.mapNodes.first()
    }

    fun computeRoute(
        originNodeId: String,
        destinationAttraction: Attraction,
        travelMode: TravelMode
    ): OfflineRoutePlan {
        val nodesById = DumingagSeedData.mapNodes.associateBy { it.id }
        val targetNodeId = destinationAttraction.mapNodeId

        if (originNodeId == targetNodeId) {
            val node = nodesById[originNodeId] ?: DumingagSeedData.mapNodes.first()
            val arriveStep = NavigationStep(
                index = 1,
                fromNodeName = node.name.en,
                toNodeName = destinationAttraction.name.en,
                distanceKm = 0.1,
                bearingLabel = LocalizedText("Here", "Ania na", "Narito", "Aquí"),
                instruction = LocalizedText(
                    en = "You are right next to ${destinationAttraction.name.en}. Walk 100m to the main entrance.",
                    ceb = "Duol na kaayo ka sa ${destinationAttraction.name.ceb}. Lakaw og 100m paingon sa entrada.",
                    tl = "Malapit ka na sa ${destinationAttraction.name.tl}. Maglakad ng 100m patungong pasukan.",
                    es = "Ya está junto a ${destinationAttraction.name.es}. Camine 100 m hasta la entrada."
                ),
                roadName = "Local Access Path"
            )
            return OfflineRoutePlan(
                originName = node.name.en,
                destination = destinationAttraction,
                travelMode = travelMode,
                totalDistanceKm = 0.1,
                estimatedMinutes = 2,
                nodePathIds = listOf(originNodeId),
                steps = listOf(arriveStep)
            )
        }

        // Build undirected adjacency list
        val adjacency = mutableMapOf<String, MutableList<Pair<String, OfflineMapEdge>>>()
        DumingagSeedData.mapEdges.forEach { edge ->
            adjacency.getOrPut(edge.fromId) { mutableListOf() }.add(edge.toId to edge)
            adjacency.getOrPut(edge.toId) { mutableListOf() }.add(edge.fromId to edge)
        }

        // Dijkstra shortest path
        val dist = mutableMapOf<String, Double>().withDefault { Double.POSITIVE_INFINITY }
        val prevNode = mutableMapOf<String, String>()
        val prevEdge = mutableMapOf<String, OfflineMapEdge>()
        val pq = PriorityQueue<Pair<String, Double>>(compareBy { it.second })

        dist[originNodeId] = 0.0
        pq.add(originNodeId to 0.0)

        while (pq.isNotEmpty()) {
            val (u, d) = pq.poll() ?: break
            if (d > dist.getValue(u)) continue
            if (u == targetNodeId) break
            adjacency[u].orEmpty().forEach { (v, edge) ->
                val alt = d + edge.distanceKm
                if (alt < dist.getValue(v)) {
                    dist[v] = alt
                    prevNode[v] = u
                    prevEdge[v] = edge
                    pq.add(v to alt)
                }
            }
        }

        val pathIds = mutableListOf<String>()
        var curr: String? = targetNodeId
        while (curr != null) {
            pathIds.add(curr)
            if (curr == originNodeId) break
            curr = prevNode[curr]
        }
        pathIds.reverse()

        val finalPath = if (pathIds.firstOrNull() == originNodeId) pathIds else listOf(originNodeId, targetNodeId)
        val steps = mutableListOf<NavigationStep>()
        var totalKm = 0.0

        for (i in 0 until finalPath.size - 1) {
            val fromId = finalPath[i]
            val toId = finalPath[i + 1]
            val fromNode = nodesById[fromId] ?: continue
            val toNode = nodesById[toId] ?: continue
            val edge = prevEdge[toId]
            val stepKm = edge?.distanceKm ?: haversineKm(fromNode.latitude, fromNode.longitude, toNode.latitude, toNode.longitude)
            totalKm += stepKm
            val bearing = bearingDirection(fromNode.latitude, fromNode.longitude, toNode.latitude, toNode.longitude)
            val road = edge?.roadName ?: "Dumingag Barangay Road"
            val distFormatted = ((stepKm * 10.0).roundToInt() / 10.0)

            val instr = buildLocalizedInstruction(
                stepNumber = i + 1,
                isLast = i == finalPath.size - 2,
                bearing = bearing,
                road = road,
                distKm = distFormatted,
                toNode = toNode,
                destination = destinationAttraction
            )

            steps.add(
                NavigationStep(
                    index = i + 1,
                    fromNodeName = fromNode.name.en,
                    toNodeName = toNode.name.en,
                    distanceKm = distFormatted,
                    bearingLabel = bearing,
                    instruction = instr,
                    roadName = road
                )
            )
        }

        val minutes = ceil((totalKm / travelMode.speedKmH) * 60.0).toInt().coerceAtLeast(2)
        val roundedTotal = ((totalKm * 10.0).roundToInt() / 10.0)

        return OfflineRoutePlan(
            originName = nodesById[originNodeId]?.name?.en ?: "Poblacion",
            destination = destinationAttraction,
            travelMode = travelMode,
            totalDistanceKm = roundedTotal,
            estimatedMinutes = minutes,
            nodePathIds = finalPath,
            steps = steps
        )
    }

    private fun buildLocalizedInstruction(
        stepNumber: Int,
        isLast: Boolean,
        bearing: LocalizedText,
        road: String,
        distKm: Double,
        toNode: OfflineMapNode,
        destination: Attraction
    ): LocalizedText {
        return if (isLast) {
            LocalizedText(
                en = "Turn ${bearing.en} onto $road for $distKm km to arrive at ${destination.name.en}.",
                ceb = "Liko paingon sa ${bearing.ceb} subay sa $road og $distKm km hangtod maabot sa ${destination.name.ceb}.",
                tl = "Lumiko patungong ${bearing.tl} sa $road nang $distKm km hanggang makarating sa ${destination.name.tl}.",
                es = "Gire hacia el ${bearing.es} por $road durante $distKm km hasta llegar a ${destination.name.es}."
            )
        } else {
            LocalizedText(
                en = "Head ${bearing.en} along $road for $distKm km toward ${toNode.name.en}.",
                ceb = "Diretso paingon sa ${bearing.ceb} subay sa $road og $distKm km paingon sa ${toNode.name.ceb}.",
                tl = "Dumiretso patungong ${bearing.tl} sa $road nang $distKm km papuntang ${toNode.name.tl}.",
                es = "Avance hacia el ${bearing.es} por $road durante $distKm km hacia ${toNode.name.es}."
            )
        }
    }

    private fun bearingDirection(lat1: Double, lon1: Double, lat2: Double, lon2: Double): LocalizedText {
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(Math.toRadians(lat2))
        val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
            sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
        val deg = (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
        return when {
            deg < 22.5 || deg >= 337.5 -> LocalizedText("North", "Amihanan", "Hilaga", "Norte")
            deg < 67.5 -> LocalizedText("Northeast", "Amihanang-Sidlakan", "Hilagang-Silangan", "Noreste")
            deg < 112.5 -> LocalizedText("East", "Sidlakan", "Silangan", "Este")
            deg < 157.5 -> LocalizedText("Southeast", "Habagatang-Sidlakan", "Timog-Silangan", "Sureste")
            deg < 202.5 -> LocalizedText("South", "Habagatan", "Timog", "Sur")
            deg < 247.5 -> LocalizedText("Southwest", "Habagatang-Kasadpan", "Timog-Kanluran", "Suroeste")
            deg < 292.5 -> LocalizedText("West", "Kasadpan", "Kanluran", "Oeste")
            else -> LocalizedText("Northwest", "Amihanang-Kasadpan", "Hilagang-Kanluran", "Noroeste")
        }
    }

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
