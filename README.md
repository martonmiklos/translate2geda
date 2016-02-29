# translate2geda
A utility for converting:

- Kicad (.mod, .lib) (refactoring currently in progress)
- Eagle (.lbr) (mostly working, minus polygons)
- BXL (.bxl) (working)
- IBIS (.ibs) (working)
- gschem symdef (working), and
- BSDL (.bsd) (working)

symbols and footprints to geda compatible formats.

The Kicad portion of the utility is based on the KicadModuleToGEDA and KicadSymbolToGEDA utilities.

This utility extends the functionality of the software classes to allow additional formats to be converted.

Export to Kicad is planned once conversion functionality is in place and satisfactorily tested. This should not be difficult, since the utility uses nanometres internally and also uses many of the dimensions and flags internally that the Kicad format is based on.

Export to Eagle is also planned, as Eagle have been good enough to use an easily parsed XML format.

BXL files are a package and vendor agnostic format that includes pad, symbol and footprint definitions 
in a single binary file encoded with adaptive Huffman encoding. The adpative Huffman decoding code was ported to Java from vala code originally written by Geert Jordaens.

BSDL files are boundary surface description language file that include a pin map which can be used to create a symbol.

IBIS files are similar in that a pin map allows a symbol to be generated.

Recent XML format Eagle .lbr files contain a set of layer definitions, packages (footprints), and symbols, but the pin mapping between symbols and footprints is defined in a "deviceset" section, to allow symbols to map to different packages. This has been dealt with by exporting an individual symbol with a pin mapping for each of the packages supported in the deviceset, with a distinct "\_FOOTPRINTNAME" appended to each of the pin mappings defined in the deviceset.

Main differences:

X and Y coordinate systems are the same in gEDA and Kicad, with Y +ve downwards, but Kicad uses +ve CW rotation and decidegrees for arcs and circles.

Both gEDA and Eagle use degrees, and CCW +ve for rotation, but in Eagle the X and Y coordinate system has Y up +ve.

BXL files have +ve up, but +ve CCW for rotation and degrees, like gEDA.

Eagle files can specify zero line widths, relying on default line width value for silk features. This utility defaults to 10mil (0.010 inch) line width if a zero line width is encountered.

Eagle can specify polygons in footprint definitions, which are not supported in geda PCB. The utility flags converted footprints with polygons that could not be converted.

Disclaimer:

This utility aims to avoid excessive reinvention of the wheel and aims to facilitate sharing of design efforts. As always, converter output is not guaranteed to be error free, and footprints should be carefully checked before using them in designs for which gerber files will be sent off for manufacture.

Usage:

Install a git client, java virtual machine, and java compiler to suit your operating system

	git clone https://github.com/erichVK5/translate2geda.git
	cd translate2geda
	javac *.java
	java translate2geda someFile.lbr

The utility will use the file ending of the provided file (.symdef, .mod, .lib, .bxl, .ibs, .bsd, etc) to determine which parser is required.

