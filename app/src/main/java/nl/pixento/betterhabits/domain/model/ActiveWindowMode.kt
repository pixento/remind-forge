package nl.pixento.betterhabits.domain.model

/**
 * How the app decides which part of the day reminders are active.
 *
 * [DO_NOT_DISTURB_OFF] follows the phone's own Do Not Disturb state, which is what a scheduled DND
 * rule ("Sleeping", "Bedtime", Samsung's "Turn on as scheduled") drives - so the user keeps one
 * schedule instead of two. The app cannot *read* that schedule's times: `getAutomaticZenRules()`
 * only returns rules the caller owns, and the system's rule belongs to package "android". It can
 * only observe whether DND is on right now, which means this mode is evaluated per tick rather
 * than baked into the next trigger time.
 */
enum class ActiveWindowMode { CUSTOM_TIMES, DO_NOT_DISTURB_OFF }
