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

BXL files are a package and vendor agnostic device description format that includes pad, symbol and footprint definitions 
in a single binary file encoded with adaptive Huffman encoding. The adaptive Huffman decoding code was ported to Java from vala code originally written by Geert Jordaens.

BSDL files are boundary surface description language files that include a pin map which can be used to create a symbol.

IBIS files are similar in that a pin map allows a symbol to be generated.

Recent XML format Eagle .lbr files contain a set of layer definitions, packages (footprints), and symbols, but the pin mapping between symbols and footprints is defined in a "deviceset" section, to allow symbols to map to different packages. This has been dealt with by exporting an individual symbol with a pin mapping for each of the packages supported in the deviceset, with a distinct "\_FOOTPRINTNAME" appended to each of the pin mappings defined in the deviceset, i.e. a symbol with three different pin mappings will result in three different symbols being generated with unique footprint=SPECIFICFP fields.

Main differences:

X and Y coordinate systems are the same in gEDA and Kicad, with Y +ve downwards, but Kicad uses +ve CW rotation and decidegrees for arcs and circles.

Both gEDA and Eagle use degrees, and CCW +ve for rotation, but in Eagle the X and Y coordinate system has Y up +ve.

BXL files have +ve up, but +ve CCW for rotation and degrees, like gEDA.

Eagle files can specify zero line widths, relying on default line width value for silk features. This utility defaults to 10mil (0.010 inch) line width if a zero line width is encountered.

Eagle can specify polygons in footprint definitions, which are not supported in geda PCB. The utility flags converted footprints with polygons that could not be converted.

Disclaimer:

This utility aims to avoid excessive reinvention of the wheel and aims to facilitate sharing of design efforts. As always, converter output is not guaranteed to be error free, and footprints should be carefully checked before using them in designs for which gerber files will be sent off for manufacture.

Issues:

- other EDA tools do not necessarily enforce sane pin spacings in their symbols, or grid aligned pins. Work is underway to flag such symbols and offer enforced grid spacing, at the risk of wrecking silk features/overall aesthetics.
- pin mappings in other EDA suites do not necessarily conform to gEDA guidelines, but replacing the pin mappings with non-text, i.e. numbers, risks a loss of information and the introduction of errors - an aim is to minimise information loss as much as possible during conversion.
- trapezoidal pads in Kicad and polygonal pads in Eagle are not supported yet, but work is underway to convert them to gEDA PCB compatible features.
- Eagle is very flexible in how it defines "slots", and a relatively foolproof way of converting Eagle "gates" into geda "slots" eludes me for now.

Usage:

Install a git client, java virtual machine, and java compiler to suit your operating system

	git clone https://github.com/erichVK5/translate2geda.git
	cd translate2geda
	javac *.java
	java translate2geda someFile.lbr

The utility will use the file ending of the provided file (.symdef, .mod, .lib, .bxl, .ibs, .bsd, etc) to determine which parser is required.

To do:

- open JSON format conversion
- Kicad import/export
- Kicad trapezoidal pad support
- Eagle polygons
- Eagle export
- flagging +/- optional enforcement of desired symbol pin spacing
- option for numerical pin mapping to be applied, over-riding source text based pin mappings
- summary file generation
