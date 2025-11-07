package `in`.tuwi.yogaphone.data

object Constants {
    const val MAX_FAVORITE_APPS = 8

    // Preference keys
    const val PREFS_NAME = "yoga_launcher_prefs"
    const val KEY_FIRST_LAUNCH = "first_launch"
    const val KEY_THEME = "theme"
    const val KEY_FONT_SIZE = "font_size"

    // Theme values
    const val THEME_LIGHT = 0
    const val THEME_DARK = 1
    const val THEME_GREYSCALE = 2

    // Font size values
    const val FONT_SIZE_SMALL = 0
    const val FONT_SIZE_MEDIUM = 1
    const val FONT_SIZE_LARGE = 2

    // Favorite app keys
    const val KEY_APP_NAME_PREFIX = "app_name_"
    const val KEY_APP_PACKAGE_PREFIX = "app_package_"
    const val KEY_APP_CLASS_PREFIX = "app_class_"
    const val KEY_APP_USER_PREFIX = "app_user_"
}
