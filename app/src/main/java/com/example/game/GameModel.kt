package com.example.game

import androidx.compose.ui.graphics.Color

data class Point(val x: Int, val y: Int) {
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    operator fun minus(other: Point) = Point(x - other.x, y - other.y)
    operator fun times(scalar: Int) = Point(x * scalar, y * scalar)
}

enum class ArrowState {
    IDLE,
    WIGGLING, // Stuck shake animation
    EXITING,  // Sliding outward
    EXITED
}

data class Arrow(
    val id: Int,
    val path: List<Point>, // Grid coordinates from tail to head
    val color: Color,
    val state: ArrowState = ArrowState.IDLE,
    val slideAnimProgress: Float = 0f, // 0f to 1f for exiting/wiggling
    val isCollided: Boolean = false
) {
    val head: Point get() = path.last()
    
    val exitDirection: Point get() {
        if (path.size < 2) return Point(0, -1) // Default up
        val pLast = path.last()
        val pPrev = path[path.size - 2]
        val dx = (pLast.x - pPrev.x).coerceIn(-1, 1)
        val dy = (pLast.y - pPrev.y).coerceIn(-1, 1)
        return Point(dx, dy)
    }
    
    // Checks if a given point is covered by this arrow's body
    fun occupies(point: Point): Boolean {
        if (state == ArrowState.EXITED) return false
        return path.contains(point)
    }
}

enum class ShapeCategory(val displayNameEn: String, val displayNameRu: String) {
    ANIMALS("Animals", "Животные"),
    BIRDS("Birds", "Птицы"),
    DINOSAURS_MONSTERS("Dinosaurs & Monsters", "Динозавры и монстры"),
    TRANSPORT("Transport", "Транспорт"),
    ARCHITECTURE("Architecture", "Архитектура"),
    FANTASY("Fantasy", "Фэнтези"),
    FOOD("Food", "Еда"),
    NATURE("Nature", "Природа"),
    SYMBOLS("Symbols", "Символы"),
    PREMIUM("Premium Shapes", "Премиум фигуры")
}

enum class ShapeType(
    val category: ShapeCategory,
    val displayName: String,
    val description: String
) {
    // ANIMALS (20)
    CAT(ShapeCategory.ANIMALS, "Cat", "A cute furry cat silhouette."),
    DOG(ShapeCategory.ANIMALS, "Dog", "A friendly playful dog shape."),
    FOX(ShapeCategory.ANIMALS, "Fox", "A clever fox with a fluffy tail."),
    WOLF(ShapeCategory.ANIMALS, "Wolf", "A majestic wild wolf howling."),
    LION(ShapeCategory.ANIMALS, "Lion", "A powerful lion with a grand mane."),
    TIGER(ShapeCategory.ANIMALS, "Tiger", "A majestic striped tiger silhouette."),
    BEAR(ShapeCategory.ANIMALS, "Bear", "A strong wild bear shape."),
    RABBIT(ShapeCategory.ANIMALS, "Rabbit", "A cute hopping rabbit with long ears."),
    MOUSE(ShapeCategory.ANIMALS, "Mouse", "A small mouse with a long tail."),
    HORSE(ShapeCategory.ANIMALS, "Horse", "A majestic running horse shape."),
    DEER(ShapeCategory.ANIMALS, "Deer", "A graceful deer with antlers."),
    ELEPHANT(ShapeCategory.ANIMALS, "Elephant", "A massive friendly elephant with a trunk."),
    GIRAFFE(ShapeCategory.ANIMALS, "Giraffe", "A tall giraffe with a long neck."),
    MONKEY(ShapeCategory.ANIMALS, "Monkey", "A playful monkey swinging."),
    PANDA(ShapeCategory.ANIMALS, "Panda", "A friendly round panda face."),
    KOALA(ShapeCategory.ANIMALS, "Koala", "A cute koala clinging to a branch."),
    DOLPHIN(ShapeCategory.ANIMALS, "Dolphin", "A playful dolphin jumping out of the water."),
    WHALE(ShapeCategory.ANIMALS, "Whale", "A giant friendly whale swimming."),
    SHARK(ShapeCategory.ANIMALS, "Shark", "A powerful ocean shark silhouette."),
    OWL(ShapeCategory.ANIMALS, "Owl", "A wise owl with large round eyes."),

    // BIRDS (10)
    EAGLE(ShapeCategory.BIRDS, "Eagle", "A majestic eagle flying high."),
    PARROT(ShapeCategory.BIRDS, "Parrot", "A colorful exotic parrot shape."),
    PENGUIN(ShapeCategory.BIRDS, "Penguin", "A cute penguin standing in the snow."),
    SWAN(ShapeCategory.BIRDS, "Swan", "An elegant swan swimming gracefully."),
    DUCK(ShapeCategory.BIRDS, "Duck", "A friendly swimming duck shape."),
    CHICKEN(ShapeCategory.BIRDS, "Chicken", "A plump farm chicken shape."),
    ROOSTER(ShapeCategory.BIRDS, "Rooster", "A proud morning rooster silhouette."),
    FLAMINGO(ShapeCategory.BIRDS, "Flamingo", "An elegant pink flamingo on one leg."),
    PEACOCK(ShapeCategory.BIRDS, "Peacock", "A proud peacock with a magnificent tail."),
    BAT(ShapeCategory.BIRDS, "Bat", "A mysterious bat flying in the night."),

    // DINOSAURS & MONSTERS (10)
    TREX(ShapeCategory.DINOSAURS_MONSTERS, "T-Rex", "A fierce Tyrannosaurus Rex shape."),
    TRICERATOPS(ShapeCategory.DINOSAURS_MONSTERS, "Triceratops", "A dinosaur with three horns."),
    STEGOSAURUS(ShapeCategory.DINOSAURS_MONSTERS, "Stegosaurus", "A dinosaur with plates on its back."),
    PTERODACTYL(ShapeCategory.DINOSAURS_MONSTERS, "Pterodactyl", "A flying dinosaur silhouette."),
    DRAGON(ShapeCategory.DINOSAURS_MONSTERS, "Dragon", "A mythical fire-breathing dragon."),
    HYDRA(ShapeCategory.DINOSAURS_MONSTERS, "Hydra", "A legendary multi-headed serpent."),
    KRAKEN(ShapeCategory.DINOSAURS_MONSTERS, "Kraken", "A giant mythical sea monster."),
    GODZILLA(ShapeCategory.DINOSAURS_MONSTERS, "Godzilla", "A giant radioactive monster."),
    WYVERN(ShapeCategory.DINOSAURS_MONSTERS, "Wyvern", "A two-legged flying dragon."),
    MINOTAUR(ShapeCategory.DINOSAURS_MONSTERS, "Minotaur", "A mythical half-man half-bull."),

    // TRANSPORT (15)
    CAR(ShapeCategory.TRANSPORT, "Car", "A classic elegant sedan car."),
    SPORT_CAR(ShapeCategory.TRANSPORT, "Sport Car", "A fast aerodynamic sports car."),
    TRUCK(ShapeCategory.TRANSPORT, "Truck", "A powerful cargo delivery truck."),
    BUS(ShapeCategory.TRANSPORT, "Bus", "A large city transit bus."),
    TRAIN(ShapeCategory.TRANSPORT, "Train", "A modern passenger train locomotive."),
    STEAM_LOCOMOTIVE(ShapeCategory.TRANSPORT, "Steam Locomotive", "A retro classic steam engine train."),
    TRACTOR(ShapeCategory.TRANSPORT, "Tractor", "A powerful farm tractor shape."),
    MOTORCYCLE(ShapeCategory.TRANSPORT, "Motorcycle", "A fast two-wheeled motorbike."),
    BICYCLE(ShapeCategory.TRANSPORT, "Bicycle", "A classic two-wheeled bicycle."),
    AIRPLANE(ShapeCategory.TRANSPORT, "Airplane", "A high-flying passenger jet."),
    HELICOPTER(ShapeCategory.TRANSPORT, "Helicopter", "A versatile helicopter with rotors."),
    ROCKET(ShapeCategory.TRANSPORT, "Rocket", "A powerful space rocket launching."),
    SPACESHIP(ShapeCategory.TRANSPORT, "Spaceship", "A futuristic intergalactic starship."),
    YACHT(ShapeCategory.TRANSPORT, "Yacht", "A luxurious ocean cruising yacht."),
    SUBMARINE(ShapeCategory.TRANSPORT, "Submarine", "A deep-diving navy submarine."),

    // ARCHITECTURE (10)
    CASTLE(ShapeCategory.ARCHITECTURE, "Castle", "A majestic medieval fortress keep."),
    TOWER(ShapeCategory.ARCHITECTURE, "Tower", "A tall elegant castle tower."),
    LIGHTHOUSE(ShapeCategory.ARCHITECTURE, "Lighthouse", "A coastal lighthouse guiding ships."),
    WINDMILL(ShapeCategory.ARCHITECTURE, "Windmill", "A classic agricultural windmill."),
    BRIDGE(ShapeCategory.ARCHITECTURE, "Bridge", "A beautiful suspension bridge spanning a river."),
    TEMPLE(ShapeCategory.ARCHITECTURE, "Temple", "An ancient architectural temple silhouette."),
    PAGODA(ShapeCategory.ARCHITECTURE, "Pagoda", "An elegant multi-tiered eastern pagoda."),
    CHURCH(ShapeCategory.ARCHITECTURE, "Church", "A classic church with a towering spire."),
    SKYSCRAPER(ShapeCategory.ARCHITECTURE, "Skyscraper", "A modern towering city skyscraper."),
    FORTRESS(ShapeCategory.ARCHITECTURE, "Fortress", "An impenetrable stone defensive fort."),

    // FANTASY (10)
    SWORD(ShapeCategory.FANTASY, "Sword", "A sharp steel fantasy sword."),
    SHIELD(ShapeCategory.FANTASY, "Shield", "A sturdy knight's protective shield."),
    CROWN(ShapeCategory.FANTASY, "Crown", "A golden royal crown decorated with jewels."),
    TREASURE_CHEST(ShapeCategory.FANTASY, "Treasure Chest", "A wooden chest full of gold and jewels."),
    MAGIC_WAND(ShapeCategory.FANTASY, "Magic Wand", "A magical wand radiating spell energy."),
    POTION(ShapeCategory.FANTASY, "Potion", "A mysterious bubbling magical potion vial."),
    KNIGHT_HELMET(ShapeCategory.FANTASY, "Knight Helmet", "A protective steel knight helmet."),
    BATTLE_AXE(ShapeCategory.FANTASY, "Battle Axe", "A heavy two-handed fantasy battle axe."),
    HAMMER(ShapeCategory.FANTASY, "Hammer", "A massive war hammer forged of steel."),
    CRYSTAL(ShapeCategory.FANTASY, "Crystal", "A glowing magical power crystal shard."),

    // FOOD (10)
    PIZZA(ShapeCategory.FOOD, "Pizza", "A delicious round slice of pizza."),
    BURGER(ShapeCategory.FOOD, "Burger", "A juicy double cheeseburger shape."),
    ICE_CREAM(ShapeCategory.FOOD, "Ice Cream", "A delicious sweet ice cream cone."),
    DONUT(ShapeCategory.FOOD, "Donut", "A sweet glazed donut with a central hole."),
    CUPCAKE(ShapeCategory.FOOD, "Cupcake", "A cute frosted cupcake with a cherry."),
    APPLE(ShapeCategory.FOOD, "Apple", "A fresh crunchy apple with a leaf."),
    STRAWBERRY(ShapeCategory.FOOD, "Strawberry", "A sweet ripe red strawberry shape."),
    WATERMELON(ShapeCategory.FOOD, "Watermelon", "A refreshing juicy watermelon slice."),
    COFFEE_CUP(ShapeCategory.FOOD, "Coffee Cup", "A warm steaming hot coffee mug."),
    BREAD(ShapeCategory.FOOD, "Bread", "A fresh loaf of golden baked bread."),

    // NATURE (10)
    TREE(ShapeCategory.NATURE, "Tree", "A classic leafy forest tree."),
    PINE_TREE(ShapeCategory.NATURE, "Pine Tree", "A tall evergreen pine tree shape."),
    PALM_TREE(ShapeCategory.NATURE, "Palm Tree", "A tropical palm tree with leafy branches."),
    FLOWER(ShapeCategory.NATURE, "Flower", "A beautiful blooming flower with petals."),
    ROSE(ShapeCategory.NATURE, "Rose", "An elegant rose with layered petals."),
    SUNFLOWER(ShapeCategory.NATURE, "Sunflower", "A grand bright sunny sunflower head."),
    MUSHROOM(ShapeCategory.NATURE, "Mushroom", "A fresh forest mushroom cap and stem."),
    CACTUS(ShapeCategory.NATURE, "Cactus", "A desert cactus with branching arms."),
    MOUNTAIN(ShapeCategory.NATURE, "Mountain", "A majestic snow-capped mountain peak."),
    VOLCANO(ShapeCategory.NATURE, "Volcano", "An erupting volcano with hot flowing lava."),

    // SYMBOLS (5)
    HEART(ShapeCategory.SYMBOLS, "Heart", "A classic beautiful romantic heart symbol."),
    STAR(ShapeCategory.SYMBOLS, "Star", "A beautiful glowing five-pointed star."),
    MOON(ShapeCategory.SYMBOLS, "Crescent Moon", "A beautiful crescent moon shape."),
    SUN(ShapeCategory.SYMBOLS, "Sun", "A bright radiant sun with warm solar rays."),
    CLOUD(ShapeCategory.SYMBOLS, "Cloud", "A soft fluffy floating weather cloud."),

    // PREMIUM (10)
    CHESS_KNIGHT(ShapeCategory.PREMIUM, "Chess Knight", "An elegant chess knight piece shape."),
    SAMURAI(ShapeCategory.PREMIUM, "Samurai", "A proud legendary samurai helmet silhouette."),
    PIRATE_SHIP(ShapeCategory.PREMIUM, "Pirate Ship", "A magnificent pirate galleon on the high seas."),
    DRAGON_HEAD(ShapeCategory.PREMIUM, "Dragon Head", "A powerful detailed dragon head profile."),
    PHOENIX(ShapeCategory.PREMIUM, "Phoenix", "A legendary firebird rising from the ashes."),
    UNICORN(ShapeCategory.PREMIUM, "Unicorn", "A magical mythical horned unicorn profile."),
    SPACE_STATION(ShapeCategory.PREMIUM, "Space Station", "A high-tech orbital space station module."),
    TREASURE_ISLAND(ShapeCategory.PREMIUM, "Treasure Island", "A remote tropical island with a palm and chest."),
    KING_CROWN(ShapeCategory.PREMIUM, "King's Crown", "A grand majestic imperial king's crown."),
    ROYAL_CASTLE(ShapeCategory.PREMIUM, "Royal Castle", "A magnificent fairy-tale royal palace castle.")
}

fun ShapeType.getLocalizedName(isEnglish: Boolean): String {
    if (isEnglish) return displayName
    return when (this) {
        // ANIMALS
        ShapeType.CAT -> "Кошка"
        ShapeType.DOG -> "Собака"
        ShapeType.FOX -> "Лиса"
        ShapeType.WOLF -> "Волк"
        ShapeType.LION -> "Лев"
        ShapeType.TIGER -> "Тигр"
        ShapeType.BEAR -> "Медведь"
        ShapeType.RABBIT -> "Кролик"
        ShapeType.MOUSE -> "Мышь"
        ShapeType.HORSE -> "Лошадь"
        ShapeType.DEER -> "Олень"
        ShapeType.ELEPHANT -> "Слон"
        ShapeType.GIRAFFE -> "Жираф"
        ShapeType.MONKEY -> "Обезьяна"
        ShapeType.PANDA -> "Панда"
        ShapeType.KOALA -> "Коала"
        ShapeType.DOLPHIN -> "Дельфин"
        ShapeType.WHALE -> "Кит"
        ShapeType.SHARK -> "Акула"
        ShapeType.OWL -> "Сова"

        // BIRDS
        ShapeType.EAGLE -> "Орел"
        ShapeType.PARROT -> "Попугай"
        ShapeType.PENGUIN -> "Пингвин"
        ShapeType.SWAN -> "Лебедь"
        ShapeType.DUCK -> "Утка"
        ShapeType.CHICKEN -> "Курица"
        ShapeType.ROOSTER -> "Петух"
        ShapeType.FLAMINGO -> "Фламинго"
        ShapeType.PEACOCK -> "Павлин"
        ShapeType.BAT -> "Летучая мышь"

        // DINOSAURS & MONSTERS
        ShapeType.TREX -> "Тираннозавр"
        ShapeType.TRICERATOPS -> "Трицератопс"
        ShapeType.STEGOSAURUS -> "Стегозавр"
        ShapeType.PTERODACTYL -> "Птеродактиль"
        ShapeType.DRAGON -> "Дракон"
        ShapeType.HYDRA -> "Гидра"
        ShapeType.KRAKEN -> "Кракен"
        ShapeType.GODZILLA -> "Годзилла"
        ShapeType.WYVERN -> "Виверна"
        ShapeType.MINOTAUR -> "Минотавр"

        // TRANSPORT
        ShapeType.CAR -> "Автомобиль"
        ShapeType.SPORT_CAR -> "Спорткар"
        ShapeType.TRUCK -> "Грузовик"
        ShapeType.BUS -> "Автобус"
        ShapeType.TRAIN -> "Поезд"
        ShapeType.STEAM_LOCOMOTIVE -> "Паровоз"
        ShapeType.TRACTOR -> "Трактор"
        ShapeType.MOTORCYCLE -> "Мотоцикл"
        ShapeType.BICYCLE -> "Велосипед"
        ShapeType.AIRPLANE -> "Самолет"
        ShapeType.HELICOPTER -> "Вертолет"
        ShapeType.ROCKET -> "Ракета"
        ShapeType.SPACESHIP -> "Космолет"
        ShapeType.YACHT -> "Яхта"
        ShapeType.SUBMARINE -> "Подлодка"

        // ARCHITECTURE
        ShapeType.CASTLE -> "Замок"
        ShapeType.TOWER -> "Башня"
        ShapeType.LIGHTHOUSE -> "Маяк"
        ShapeType.WINDMILL -> "Мельница"
        ShapeType.BRIDGE -> "Мост"
        ShapeType.TEMPLE -> "Храм"
        ShapeType.PAGODA -> "Пагода"
        ShapeType.CHURCH -> "Церковь"
        ShapeType.SKYSCRAPER -> "Небоскреб"
        ShapeType.FORTRESS -> "Крепость"

        // FANTASY
        ShapeType.SWORD -> "Меч"
        ShapeType.SHIELD -> "Щит"
        ShapeType.CROWN -> "Корона"
        ShapeType.TREASURE_CHEST -> "Сундук"
        ShapeType.MAGIC_WAND -> "Волшебная палочка"
        ShapeType.POTION -> "Зелье"
        ShapeType.KNIGHT_HELMET -> "Шлем"
        ShapeType.BATTLE_AXE -> "Секира"
        ShapeType.HAMMER -> "Молот"
        ShapeType.CRYSTAL -> "Кристалл"

        // FOOD
        ShapeType.PIZZA -> "Пицца"
        ShapeType.BURGER -> "Бургер"
        ShapeType.ICE_CREAM -> "Мороженое"
        ShapeType.DONUT -> "Пончик"
        ShapeType.CUPCAKE -> "Кекс"
        ShapeType.APPLE -> "Яблоко"
        ShapeType.STRAWBERRY -> "Клубника"
        ShapeType.WATERMELON -> "Арбуз"
        ShapeType.COFFEE_CUP -> "Чашка кофе"
        ShapeType.BREAD -> "Хлеб"

        // NATURE
        ShapeType.TREE -> "Дерево"
        ShapeType.PINE_TREE -> "Сосна"
        ShapeType.PALM_TREE -> "Пальма"
        ShapeType.FLOWER -> "Цветок"
        ShapeType.ROSE -> "Роза"
        ShapeType.SUNFLOWER -> "Подсолнух"
        ShapeType.MUSHROOM -> "Гриб"
        ShapeType.CACTUS -> "Кактус"
        ShapeType.MOUNTAIN -> "Гора"
        ShapeType.VOLCANO -> "Вулкан"

        // SYMBOLS
        ShapeType.HEART -> "Сердце"
        ShapeType.STAR -> "Звезда"
        ShapeType.MOON -> "Полумесяц"
        ShapeType.SUN -> "Солнце"
        ShapeType.CLOUD -> "Облако"

        // PREMIUM
        ShapeType.CHESS_KNIGHT -> "Конек"
        ShapeType.SAMURAI -> "Самурай"
        ShapeType.PIRATE_SHIP -> "Пиратский корабль"
        ShapeType.DRAGON_HEAD -> "Голова дракона"
        ShapeType.PHOENIX -> "Феникс"
        ShapeType.UNICORN -> "Единорог"
        ShapeType.SPACE_STATION -> "Космическая станция"
        ShapeType.TREASURE_ISLAND -> "Остров сокровищ"
        ShapeType.KING_CROWN -> "Королевская корона"
        ShapeType.ROYAL_CASTLE -> "Королевский дворец"
    }
}

fun ShapeType.getLocalizedDescription(isEnglish: Boolean): String {
    if (isEnglish) return description
    return when (this) {
        // ANIMALS
        ShapeType.CAT -> "Милый силуэт пушистой кошки."
        ShapeType.DOG -> "Дружелюбная и игривая фигура собаки."
        ShapeType.FOX -> "Хитрая лисица с пушистым хвостом."
        ShapeType.WOLF -> "Величественный дикий волк, воющий на луну."
        ShapeType.LION -> "Могучий лев с великолепной гривой."
        ShapeType.TIGER -> "Величественный полосатый тигр."
        ShapeType.BEAR -> "Сильный дикий медведь."
        ShapeType.RABBIT -> "Милый прыгающий кролик с длинными ушами."
        ShapeType.MOUSE -> "Маленькая мышка с длинным хвостиком."
        ShapeType.HORSE -> "Величественный бегущий конь."
        ShapeType.DEER -> "Изящный благородный олень с рогами."
        ShapeType.ELEPHANT -> "Огромный дружелюбный слон с хоботом."
        ShapeType.GIRAFFE -> "Высокий жираф с длинной шеей."
        ShapeType.MONKEY -> "Игривая обезьянка, качающаяся на ветке."
        ShapeType.PANDA -> "Добродушная круглая панда."
        ShapeType.KOALA -> "Милая коала, обнимающая дерево."
        ShapeType.DOLPHIN -> "Веселый дельфин, выпрыгивающий из воды."
        ShapeType.WHALE -> "Гигантский дружелюбный кит."
        ShapeType.SHARK -> "Мощный силуэт океанской акулы."
        ShapeType.OWL -> "Мудрая сова с большими круглыми глазами."

        // BIRDS
        ShapeType.EAGLE -> "Величественный орел в высоком полете."
        ShapeType.PARROT -> "Яркий экзотический попугай."
        ShapeType.PENGUIN -> "Милый пингвин, стоящий на снегу."
        ShapeType.SWAN -> "Изящный лебедь, плывущий по воде."
        ShapeType.DUCK -> "Дружелюбная плывущая уточка."
        ShapeType.CHICKEN -> "Круглая домашняя курочка."
        ShapeType.ROOSTER -> "Гордый утренний петух."
        ShapeType.FLAMINGO -> "Грациозный розовый фламинго на одной ноге."
        ShapeType.PEACOCK -> "Великолепный павлин с роскошным хвостом."
        ShapeType.BAT -> "Таинственная летучая мышь в ночном небе."

        // DINOSAURS & MONSTERS
        ShapeType.TREX -> "Грозный тираннозавр Рекс."
        ShapeType.TRICERATOPS -> "Динозавр с тремя мощными рогами."
        ShapeType.STEGOSAURUS -> "Ящер с пластинами на спине."
        ShapeType.PTERODACTYL -> "Летающий древний ящер."
        ShapeType.DRAGON -> "Мифический огнедышащий дракон."
        ShapeType.HYDRA -> "Легендарный многоголовый змей."
        ShapeType.KRAKEN -> "Гигантское мифическое морское чудовище."
        ShapeType.GODZILLA -> "Радиоактивный гигантский монстр."
        ShapeType.WYVERN -> "Двуногий крылатый дракон."
        ShapeType.MINOTAUR -> "Мифический полубык-получеловек."

        // TRANSPORT
        ShapeType.CAR -> "Классический элегантный седан."
        ShapeType.SPORT_CAR -> "Быстрый аэродинамичный спорткар."
        ShapeType.TRUCK -> "Мощный грузовик для перевозок."
        ShapeType.BUS -> "Большой городской автобус."
        ShapeType.TRAIN -> "Современный пассажирский поезд."
        ShapeType.STEAM_LOCOMOTIVE -> "Ретро-паровоз классической формы."
        ShapeType.TRACTOR -> "Мощный сельскохозяйственный трактор."
        ShapeType.MOTORCYCLE -> "Быстрый двухколесный мотоцикл."
        ShapeType.BICYCLE -> "Классический двухколесный велосипед."
        ShapeType.AIRPLANE -> "Пассажирский авиалайнер в небе."
        ShapeType.HELICOPTER -> "Маневренный вертолет с винтами."
        ShapeType.ROCKET -> "Мощная космическая ракета на взлете."
        ShapeType.SPACESHIP -> "Футуристический межзвездный корабль."
        ShapeType.YACHT -> "Роскошная океанская яхта."
        ShapeType.SUBMARINE -> "Глубоководная военная подводная лодка."

        // ARCHITECTURE
        ShapeType.CASTLE -> "Величественный средневековый замок."
        ShapeType.TOWER -> "Высокая изящная башня."
        ShapeType.LIGHTHOUSE -> "Маяк, освещающий путь кораблям."
        ShapeType.WINDMILL -> "Классическая ветряная мельница."
        ShapeType.BRIDGE -> "Красивый висячий мост через реку."
        ShapeType.TEMPLE -> "Древний величественный храм."
        ShapeType.PAGODA -> "Изящная многоярусная восточная пагода."
        ShapeType.CHURCH -> "Классическая церковь с высоким шпилем."
        ShapeType.SKYSCRAPER -> "Современный высотный небоскреб."
        ShapeType.FORTRESS -> "Неприступный каменный форт."

        // FANTASY
        ShapeType.SWORD -> "Острый стальной фэнтезийный меч."
        ShapeType.SHIELD -> "Прочный рыцарский защитный щит."
        ShapeType.CROWN -> "Золотая королевская корона с драгоценными камнями."
        ShapeType.TREASURE_CHEST -> "Деревянный сундук, полный золота и сокровищ."
        ShapeType.MAGIC_WAND -> "Волшебная палочка, излучающая магию."
        ShapeType.POTION -> "Таинственный флакон с бурлящим зельем."
        ShapeType.KNIGHT_HELMET -> "Защитный стальной шлем рыцаря."
        ShapeType.BATTLE_AXE -> "Тяжелая двуручная боевая секира."
        ShapeType.HAMMER -> "Огромный боевой молот."
        ShapeType.CRYSTAL -> "Светящийся магический кристалл."

        // FOOD
        ShapeType.PIZZA -> "Аппетитный круглый кусочек пиццы."
        ShapeType.BURGER -> "Сочный двойной чизбургер."
        ShapeType.ICE_CREAM -> "Вкусный сладкий рожок мороженого."
        ShapeType.DONUT -> "Сладкий глазированный пончик с дырочкой."
        ShapeType.CUPCAKE -> "Праздничный кекс с вишенкой."
        ShapeType.APPLE -> "Свежее хрустящее яблоко с листиком."
        ShapeType.STRAWBERRY -> "Сладкая спелая клубника."
        ShapeType.WATERMELON -> "Освежающий сочный ломтик арбуза."
        ShapeType.COFFEE_CUP -> "Теплая ароматная чашка кофе."
        ShapeType.BREAD -> "Свежая буханка золотистого хлеба."

        // NATURE
        ShapeType.TREE -> "Классическое лиственное лесное дерево."
        ShapeType.PINE_TREE -> "Высокая вечнозеленая сосна."
        ShapeType.PALM_TREE -> "Тропическая пальма с раскидистыми ветвями."
        ShapeType.FLOWER -> "Прекрасный цветущий цветок с лепестками."
        ShapeType.ROSE -> "Изящная роза со слоистыми лепестками."
        ShapeType.SUNFLOWER -> "Яркий солнечный подсолнух."
        ShapeType.MUSHROOM -> "Лесной гриб со шляпкой и ножкой."
        ShapeType.CACTUS -> "Пустынный кактус с колючими ветвями."
        ShapeType.MOUNTAIN -> "Величественная заснеженная вершина горы."
        ShapeType.VOLCANO -> "Извергающийся вулкан с лавой."

        // SYMBOLS
        ShapeType.HEART -> "Классическое романтическое сердце."
        ShapeType.STAR -> "Красивая пятиконечная звезда."
        ShapeType.MOON -> "Изящный силуэт полумесяца."
        ShapeType.SUN -> "Яркое лучистое солнце."
        ShapeType.CLOUD -> "Мягкое пушистое облако."

        // PREMIUM
        ShapeType.CHESS_KNIGHT -> "Величественная фигура шахматного коня."
        ShapeType.SAMURAI -> "Легендарный шлем японского самурая."
        ShapeType.PIRATE_SHIP -> "Великолепный пиратский галеон."
        ShapeType.DRAGON_HEAD -> "Детальный профиль головы дракона."
        ShapeType.PHOENIX -> "Огненная птица Феникс, возрождающаяся из пепла."
        ShapeType.UNICORN -> "Волшебный единорог из сказок."
        ShapeType.SPACE_STATION -> "Орбитальный отсек космической станции."
        ShapeType.TREASURE_ISLAND -> "Далекий остров сокровищ с пальмой."
        ShapeType.KING_CROWN -> "Роскошная императорская корона."
        ShapeType.ROYAL_CASTLE -> "Сказочный королевский дворец."
    }
}

data class LevelData(
    val levelIndex: Int,
    val shapeType: ShapeType,
    val gridWidth: Int,
    val gridHeight: Int,
    val arrows: List<Arrow>,
    val generationStats: String = ""
)
