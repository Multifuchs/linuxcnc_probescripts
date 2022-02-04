package de.mf.ngcpost

import kotlin.io.path.*
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val lcArgs = args.map { it.lowercase() }
    if (args.isEmpty() || lcArgs.contains("help")
        || lcArgs.contains("-help") || lcArgs.contains("--help")
        || lcArgs.contains("-h")
    ) {
        printHelp()
    }

    val flags = args
        .filter { it.startsWith("-") }
        .map { it.trimStart('-').trim() }
        .filter { it.isNotEmpty() }
        .toSet()

    val srcDir = Path(
        args.lastOrNull { !it.startsWith("-") }
            ?: printError("No file or directory.")
    )
    try {
        srcDir.absolute()
    } catch (e: Exception) {
        printError("Cannot find path: $srcDir")
    }

    // Determine output directory.
    val iOfO = args.indexOf("-o")
    val outputDir = iOfO
        .takeIf { it != -1 && it < args.lastIndex }
        ?.let {
            val p = Path(args[it + 1])
            if (!p.isDirectory())
                p.createDirectories()
            p
        }
        ?: srcDir.let {
            if (it.isDirectory()) it
            else it.parent
        }

    val files = when {
        srcDir.isDirectory() -> srcDir.listDirectoryEntries()
            .filter { it.isRegularFile() && it.extension.lowercase() == "ngc" }
            .filter {
                val n = it.name.removeSuffix(".ngc")
                when {
                    n.endsWith(".post") -> false
                    n.endsWith(".pre") -> true
                    (it.parent / "$n.pre.ngc").exists() -> false // Do not process files, which are the output of an *pre* file.
                    else -> true
                }

            }
        srcDir.isRegularFile() -> listOf(srcDir)
        else -> printError("Cannot open $srcDir.")
    }

    if (files.isEmpty()) printError("No *.ngc files found.")

    files.forEach { file ->
        try {
            val preFileName = file.name.removeSuffix(".pre.ngc")
            val ngcFileName = file.name.removeSuffix(".ngc")
            val newFileName = when {
                !srcDir.isSameFileAs(outputDir) -> file.name
                preFileName != file.name -> "$preFileName.ngc" // Just remove .pre.
                else -> "$ngcFileName.post.ngc" // Add .post.
            }
            val newFile = outputDir / newFileName

            if (!flags.contains("q")) println("Process file $file -> $newFile ...")

            file.reader().buffered().use { reader ->
                newFile.writer().buffered().use { writer ->
                    postprocessFile(reader, writer, flags)
                }
            }
        } catch (e: Exception) {
            if (flags.contains("stacktrace")) e.printStackTrace()
            printError("Failed to process file:\n$file\n${e.message}")
        }
    }

    if (!flags.contains("q")) println("Done")
}

private fun printHelp() {
    println(
        """Usage: ngc-post [arguments] [file|directory]
Post-processes a single linuxcnc g-code file or all files matching *.ngc in the
given directory.
The post processed file(s) will be created next to their originals.
The new filename depends on the file extension of the old one:
   - *.pre.ngc -> *.ngc
   - *.ngc -> *.post.ngc
If an output directory is specified, the filenames won't be changed.
Existing files will be overwritten.

Arguments:
   -o [directory]  Sets the output directory.
   -h              Print help (this message)
   -q              Quiet mode (no output to stdout)
   --keep-indent   Keeps indentation as it is.
   --keep-olabels  Keeps O-labels as they are.
   --with-percent  Adds % as the first and last line."""
    )
    exitProcess(0)
}

private fun printError(str: String): Nothing {
    System.err.println(str)
    exitProcess(1)
}