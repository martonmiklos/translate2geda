# translate2geda
A utility for converting:

- Kicad (.mod, .lib) (refactoring in progress, please use kicadModule2geda or kicadSymbol2geda)
- Eagle (.lbr) (mostly working, minus polygons)
- BXL (.bxl) (working)
- IBIS (.ibs) (working)
- gschem symdef (working), and
- BSDL (.bsd) (working)
- LT-Spice (.asc) (working)
- QUCS (.sch) (nets convert, and preliminary symbol support working)
- gerber file (.gbr, .gbo, .gto, .gbs, .gts, .gbl, .gtl, .pho) ... a preliminary implementation

symbols and footprints and LT-Spice schematics and QUCS schematics and gerbers to geda compatible formats.

The Kicad portion of the utility is based on the KicadModuleToGEDA and KicadSymbolToGEDA utilities.

This utility extends the functionality of the software classes to allow additional formats to be converted.

Export to Kicad is planned once conversion functionality is in place and satisfactorily tested. This should not be difficult, since the utility uses nanometres internally and also uses many of the dimensions and flags internally that the Kicad format is based on.

Export to Eagle is also planned, as Eagle have been good enough to use an easily parsed XML format.

BXL files are a package and vendor agnostic device description format that includes pad, symbol and footprint definitions in a single binary file encoded with adaptive Huffman encoding. The adaptive Huffman decoding code was ported to Java from vala code originally written by Geert Jordaens.

Gerber files are parsed and decoded by code originally written by Phillip Knirsch to render and print gerbers: 
http://www.wizards.de/phil/java/rs274x.html 
The code has been modified to generate pins/pads and traces as footprint elements, and now detects inch/mm units. More work needs to be done to convert the gerber parsing routines to long int nanometres, unbreak the graphics exporting, and possibly generate PCB layout files which try to put traces into their own layer, distinct from footprint elements, although the heuristics will be subject to high false positive rates in some cases due to some EDA tools rendering pins/pads/polygons with painted strokes, instead of flashed apertures or polygons. Gerber elements are individually numbered, so that mouse over actions which show the pin number allow groups of pins to be identified with simple searches in the text .fp file and copied/combined if extracting device footprints from larger gerbers, etc...

BSDL files are boundary surface description language files that include a pin map which can be used to create a symbol.

IBIS files are similar in that a pin map allows a symbol to be generated.

Recent XML format Eagle .lbr files contain a set of layer definitions, packages (footprints), and symbols, but the pin mapping between symbols and footprints is defined in a "deviceset" section, to allow symbols to map to different packages. This has been dealt with by exporting an individual symbol with a pin mapping for each of the packages supported in the deviceset, with a distinct "\_FOOTPRINTNAME" appended to each of the pin mappings defined in the deviceset, i.e. a symbol with three different pin mappings will result in three different symbols being generated with unique footprint=SPECIFICFP fields.

LT-Spice .asc files are text files exported by LT-Spice and capture the schematic used in LT-Spice for circuit modelling. The .asc file contains "WIRE"s which connect discrete components, references to component symbols, and attributes for the components such as their value and refdes.

QUCS .sym files are text files exported by QUCS (Quite Universal Circuit Simulator) and capture the schematic used in QUCS for circuit modelling. The .sch file contains "WIRE"s which connect discrete components, references to component symbols, and attributes for the components such as their value and refdes.

Main differences:

X and Y coordinate systems are the same in gEDA and Kicad, with Y +ve downwards, but Kicad uses +ve CW rotation and decidegrees for arcs and circles.

Both gEDA and Eagle use degrees, and CCW +ve for rotation, but in Eagle the X and Y coordinate system has Y up +ve.

BXL files have +ve up, but +ve CCW for rotation and degrees, like gEDA.

Eagle files can specify zero line widths, relying on default line width value for silk features. This utility defaults to 10mil (0.010 inch) line width if a zero line width is encountered.

Eagle can specify polygons in footprint definitions, which are not supported in geda PCB. The utility flags converted footprints with polygons that could not be converted.

LT-Spice has +ve Y down, unlike gschem. Rotation is in the opposite direction to gschem. The grid in LT-Spice increase in mutiples of 16, and a conversion factor of 12.5 achieves pin spacings which are multiples of 100, and suited to gschem. For the WIREs in LT-Spice to connect properly in the converted gschem schematic, custom gschem symbols have been generated which match the dimensions and pinouts of the default LT-Spice components. These end in -LTS.sym, and need to be in the default search path of gschem when gschem is used to view and edit the converted schematic.

QUCs has +ve Y down, unlike gschem. Rotation is +ve CCW. Grid increments are in units of 10, and most symbols have pin spacings of 60, allowing for a magnification factor of 10 to be applied. Custom symbols also need to be installed in the gschem search path, as with LT-Spice. 

Disclaimer:

This utility aims to avoid excessive reinvention of the wheel and aims to facilitate sharing of design efforts. As always, converter output is not guaranteed to be error free, and footprints should be carefully checked before using them in designs for which gerber files will be sent off for manufacture.

Issues:

- other EDA tools do not necessarily enforce sane pin spacings in their symbols, or grid aligned pins. Work is underway to flag such symbols and offer enforced grid spacing, at the risk of wrecking silk features/overall aesthetics.
- pin mappings in other EDA suites do not necessarily conform to gEDA guidelines, but replacing the pin mappings with non-text, i.e. numbers, risks a loss of information and the introduction of errors - an aim is to minimise information loss as much as possible during conversion.
- trapezoidal pads in Kicad and polygonal pads in Eagle are not supported yet, but work is underway to convert them to gEDA PCB compatible features.
- Eagle is very flexible in how it defines "slots", and a relatively foolproof way of converting Eagle "gates" into geda "slots" eludes me for now.
- LTSpice components have their position, value and refdes converted.
- QUCS components have their position and refdes converted, but component values are only ported for resistors, inductors and capacitors.
- BXL conversion uses Adaptive Huffman Decoding. This takes a lot of shuffling of nodes within trees. Plan to wander off and make some coffee while it decodes.
- QUCS compatible symbols included in the symbols directory should have the correct geometry in the converted schematic, but pinouts need to be checked before proceeding to allocate footprints and generating a PCB layout, since QUCS is not very explicit about which physical pin goes where.
- gerber conversion is broken if the utility is compiled with the gjc utility to a native binary. 

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
- improve the aesthetics of the placement of the ported LTSpice refdes vs the symbol

How to generate additional LT-Spice compatible symbols:

If translate2geda is unaware of a symbol description, the converted schematic will have an "unknown-LTS.sym" placed at the position of the unknown symbol.

The next step is to load an equivalent gschem symbol which is a very close, or ideally, exact, match for the pin geometry of the missing component. Once placed in position, the symbol should be highlighted, and "e b" pressed to embed the component in the schematic, and the schematic then saved.

The schematic should then be copied to another file "mynewsymbol-LTS.sym", and opened in an editor. The first line of the schematic file should be preserved, but everything other than the embedded component descriptions between the "[" and "]" brackets deleted. The file is then saved.

The file "mynewsymbol-LTS.sym" is then opened in gschem. The symbol is selected with select all, cut, and after using the scroll bars to get to the origin at the lower left corner of the screen, the symbol can be pasted close to the origin. The symbol is then saved.

A copy of the symbol is then placed in gschem's symbol search path.

The converted schematic is then loaded, after changing "unknown-LTS.sym" to the new "mynewsymbol-LTS.sym" within the schematic file. If lucky, the new symbol's origin will match that needed for the schematic. If not, take note of the (x,y) offset required to place it properly, and/or any lengthening, shortening or translation of pins required to effect a match, and undertake this again in gschem on the "mynewsymbol-LTS.sym" file, saving it again after modification. This can be tricky, as gschem does not show a symbol placed in the negative portions of the screen, and you will have to drag and drop it off outside of the display area to some extent. Alternatively, the text file can be manually edited.

Reload gschem to view the converted schematic, and if all is well, you now have a matching gschem symbol. Ideally, translate2geda.java should be modified and recompiled to recognise the new symbol, to automate things subsequently.

A similar process can be used to generate new symbols for schematics exported from QUCS.

How to build a native binary with gcj:

This has now been achieved. The gnu gcj compiler is less permissive than the usual jdk javac, and the use of hasNext() in the gjc library behaves differently to that in the standard jdk libraries, and can return TRUE but then lead to a null when nextLine() is called, which then cause subsequent null pointer exceptions when trim() or split() are called. Modifications to the code have been made to deal with this difference, namely, hasNextLine() is now used instead.

To compile a native binary, perhaps because you want to use it compactly in a cgi application:

	sudo apt-get install gcj-jdk
	gcj -I src -C *.java
	gcj -I src --main=translate2geda *.class -o testing.out
	./testing someThingToConvert.file

This has been tested successfully on:

	Ubuntu 14.04.4 LTS
	/usr/lib/x86_64-linux-gnu/libgcj.so.14

