package dacslab.heterosync.core

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform