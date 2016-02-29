// translate2geda.java - a utility for converting various EDA file
// formats to geda PCB footprints and gschem symbols
//
// translate2geda.java v1.0
// Copyright (C) 2016 Erich S. Heinzle, a1039181@gmail.com

//    see LICENSE-gpl-v2.txt for software license
//    see README.txt
//    
//    This program is free software; you can redistribute it and/or
//    modify it under the terms of the GNU General Public License
//    as published by the Free Software Foundation; either version 2
//    of the License, or (at your option) any later version.
//    
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//    
//    You should have received a copy of the GNU General Public License
//    along with this program; if not, write to the Free Software
//    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
//    
//    translate2geda Copyright (C) 2016 Erich S. Heinzle a1039181@gmail.com


import java.io.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class translate2geda {

  static boolean verbose = false;

  public static void main (String [] args) {

    boolean textOutputOnly = false;
    boolean quietMode = false;
    String filename = "";
    String [] convertedFiles = null;

    if (args.length == 0) {
      printHelp();
      System.exit(0);
    } else {
      filename = args[0];
      for (String arg : args) {
        if (arg.equals("-t")) {
          textOutputOnly = true;
        } else if (arg.equals("-q")){
          quietMode = true;
        } else if (arg.equals("-v")){
          verbose = true;
        }
      }
    }

    if (!quietMode) {
      System.out.println("Using filename: " + filename);
    }

    // we'll now try and decide what to do with the supplied file
    // based on the file ending

    if (filename.endsWith(".bsd") ||
        filename.endsWith(".BSD")) {
      try {
        convertedFiles = parseBSDL(filename);
      } catch (Exception e) {
        defaultFileIOError(e);
      }
    } else if ((filename.endsWith(".bxl") ||
                filename.endsWith(".BXL")) && 
               textOutputOnly)  {
      textOnlyBXL(filename);
      System.exit(0);
    } else if (filename.endsWith(".bxl") ||
               filename.endsWith(".BXL"))  {
      try {
        convertedFiles = parseBXL(filename);
      } catch (Exception e) {
        defaultFileIOError(e);
      }
    } else if (filename.endsWith(".ibs") ||
               filename.endsWith(".IBS") ) {
      try {
        convertedFiles = parseIBIS(filename);
      } catch (Exception e) {
        defaultFileIOError(e);
      }
    } else if (filename.endsWith(".symdef") ||
               filename.endsWith(".SYMDEF") ) {
      try {
        convertedFiles = parseSymdef(filename);
      } catch (Exception e) {
        defaultFileIOError(e);
      }
    } else if (filename.endsWith(".lbr") ||
               filename.endsWith(".LBR") ) {
      try {
        convertedFiles = parseEagleLBR(filename);
      } catch (Exception e) {
        defaultFileIOError(e);
      }
    } else {
      System.out.println("I didn't recognise a suitable file " +
                         "ending for conversion, i.e..\n" +
                         "\t.bxl, .bsd, .ibs, .symdef" );
    }

    if (convertedFiles != null &&
        !quietMode) {
      for (String converted : convertedFiles) {
        System.out.println(converted);
      }
    }

  }

  // Eagle libraries provide footprint data, pin mapping, and
  // schematic symbol data

  private static String [] parseEagleLBR(String LBRFile) throws IOException {
    File EagleLBR = new File(LBRFile);
    Scanner eagleLib = new Scanner(EagleLBR);

    String currentLine = "";
    String newElement = "";
    String newSymbol = "";
    String symAttributes = "";
    String elData = "";
    String elName = "";
    EagleLayers layers = new EagleLayers();
    EagleDeviceSet deviceSets = null;

    PinList pins = new PinList(0); // slots = 0

    List<String> convertedFiles = new ArrayList<String>();
    ArrayList<String> layerDefs = new ArrayList<String>();
    ArrayList<String> packageDefs = new ArrayList<String>();
    ArrayList<String> symbolDefs = new ArrayList<String>();
    ArrayList<String> deviceSetDefs = new ArrayList<String>();

    List<String> currentPackage = new ArrayList<String>();

    long xOffset = 0;
    long yOffset = 0; // used to justify symbol
    long textXOffset = 0; // used for attribute fields

    while (eagleLib.hasNext()) {
      currentLine = eagleLib.nextLine().trim();
      if (currentLine.startsWith("<layers>")) {
        while (eagleLib.hasNext() &&
               !currentLine.startsWith("</layers>")) {
          currentLine = eagleLib.nextLine().trim();
          layerDefs.add(currentLine);
        }
      } else if (currentLine.startsWith("<packages>")) {
        while (eagleLib.hasNext() &&
              !currentLine.startsWith("</packages>")) {
          currentLine = eagleLib.nextLine().trim();
          packageDefs.add(currentLine);
        }
      } else if (currentLine.startsWith("<symbols>")) {
        while (eagleLib.hasNext() &&
              !currentLine.startsWith("</symbols>")) {
          currentLine = eagleLib.nextLine().trim();
          symbolDefs.add(currentLine);
        }
      } else if (currentLine.startsWith("<devicesets>")) {
        currentLine = eagleLib.nextLine().trim();
        while (eagleLib.hasNext() &&
              !currentLine.startsWith("</devicesets>")) {
          if (currentLine.startsWith("<deviceset ") &&
              eagleLib.hasNext()) {
            String currentGates = "";
            currentLine = eagleLib.nextLine().trim();
            while (eagleLib.hasNext() &&
                   !currentLine.startsWith("</deviceset>")) {
              if (currentLine.startsWith("<gates>")) {
                currentGates = currentLine + "\n";
                currentLine = eagleLib.nextLine().trim();
                while (eagleLib.hasNext() &&
                       !currentLine.startsWith("</gates>")) {
                  currentGates = currentGates + currentLine + "\n";
                  // System.out.println("Found some gates");
                  currentLine = eagleLib.nextLine().trim();
                }
                currentGates = currentGates + currentLine + "\n";
                //System.out.println("Found a set of gates");
              } else if (currentLine.startsWith("<device ")) {
                String currentDef = currentLine + "\n";
                // System.out.println("Found a device set line");
                currentLine = eagleLib.nextLine().trim();
                while (eagleLib.hasNext() &&
                       !currentLine.startsWith("</device>")) {
                  currentDef = currentDef + currentLine + "\n";
                  // System.out.println("Found a device set line");
                  currentLine = eagleLib.nextLine().trim();
                }
                currentDef = currentGates +
                    currentDef + currentLine + "\n";
                deviceSetDefs.add(currentDef);
                //System.out.println("Found a device set:");
                //System.out.println(currentDef);
              }
              currentLine = eagleLib.nextLine().trim();
            }
          }
          currentLine = eagleLib.nextLine().trim(); //resume while loop
        }
      }
    }

    // first, we create the layer definition object
    // which is used for extraction of other elements
    if (layerDefs.size() == 0) {
      System.out.println("This eagle library appears to be missing "
                         + "layer definitions needed for conversion");
    }
    layers = new EagleLayers(layerDefs);

    // we now turn our ArrayList into a string to pass to a scanner
    // object
    String packageDefString = "";
    for (String packageLine : packageDefs) {
      packageDefString = packageDefString + packageLine + "\n";
    }    

    // next, we parse and process the package (=FP )defs
    if (verbose) {
      System.out.println("Moving onto FPs");
    }

    Scanner packagesBundle = new Scanner(packageDefString);

    while (packagesBundle.hasNext()) {
      currentLine = packagesBundle.nextLine().trim();
      if (currentLine.startsWith("<package name")) {
        String [] tokens = currentLine.split(" ");
        String FPName
            = tokens[1].replaceAll("[\">=/]","").substring(4);
        while (packagesBundle.hasNext() &&
               !currentLine.startsWith("</package>")) {
          currentLine = packagesBundle.nextLine().trim();
          if (currentLine.startsWith("<smd") ||
              currentLine.startsWith("<pad") ||
              currentLine.startsWith("<hole")) {
            Pad newPad = new Pad();
            newPad.populateEagleElement(currentLine);
            newElement = newElement
                + newPad.generateGEDAelement(xOffset,yOffset,1.0f);
          } else if (currentLine.startsWith("<wire") &&
                     layers.isDrawnTopSilk(currentLine)) {
            if (!currentLine.contains("curve=")) {
              DrawnElement silkLine = new DrawnElement();
              silkLine.populateEagleElement(currentLine);
              newElement = newElement
                  + silkLine.generateGEDAelement(xOffset,yOffset,1.0f);
            } else {
              Arc silkArc = new Arc();
              silkArc.populateEagleElement(currentLine);
              //System.out.println("Arc element found");
              newElement = newElement
                  + silkArc.generateGEDAelement(xOffset,yOffset,1.0f);
              
            }
          } else if (currentLine.startsWith("<rectangle") &&
                     layers.isDrawnTopSilk(currentLine)) {
            DrawnElement [] silkLines
                = DrawnElement.eagleRectangleAsLines(currentLine);
            for (DrawnElement side : silkLines) {
              newElement = newElement
                  + side.generateGEDAelement(xOffset,yOffset,1.0f);
            }
          } else if (currentLine.startsWith("<circle") &&
                     layers.isDrawnTopSilk(currentLine)) {
            Circle silkCircle = new Circle();
            silkCircle.populateEagleElement(currentLine);
            //System.out.println("Arc element found");
            newElement = newElement
                + silkCircle.generateGEDAelement(xOffset,yOffset,1.0f);
          } else if (currentLine.startsWith("<polygon") &&
                     (layers.isTopCopper(currentLine) ||
                      layers.isBottomCopper(currentLine) ||
                      layers.isDrawnTopSilk(currentLine))) {
            System.out.println("Polygon omitted in: " + FPName + "\n\t"
                               +  currentLine);
          }

          
        } // end if for "<package name"
        
        // we now build the geda PCB footprint
        elData = "Element[\"\" \""
            + FPName
            + "\" \"\" \"\" 0 0 0 25000 0 100 \"\"]\n(\n"
            + newElement
            + ")";
        elName = FPName + ".fp";
        
        // we now write the element to a file
        elementWrite(elName, elData);
        // add the FP to our list of converted elements
        convertedFiles.add(elName); 
        newElement = ""; // reset the variable for batch conversion
      } // end of this particular package while loop
    } // end of packagesBundle while loop


    // we now create the set of eagle devices from which pin mappings
    // can be retrieved
    if (deviceSetDefs.size() != 0) {
      if (verbose) {
        System.out.println("About to create EagleDeviceSet object"); 
      }
      deviceSets = new EagleDeviceSet(deviceSetDefs);
      if (verbose) {
        System.out.println("Created EagleDeviceSet object");
      }
    } // we leave it as null if none found during parsing

    if (verbose) {
      System.out.println("About to process SymbolDefs"); 
    }
    // we now turn our symbol ArrayList into a string to pass
    // to a scanner object
    // StringBuilder might perform better here...
    String symbolDefString = "";
    for (String symbolLine : symbolDefs) {
      symbolDefString = symbolDefString + symbolLine + "\n";
    }

    Scanner symbolBundle = new Scanner(symbolDefString);

    // next, we parse and process the package (=FP )defs
    if (verbose) {
      System.out.println("About to create individual symbols"); 
    }
    
    while (symbolBundle.hasNext()) {
      currentLine = symbolBundle.nextLine().trim();
      if (currentLine.startsWith("<symbol ")) {
        String [] tokens = currentLine.split(" ");
        String symbolName // name="......"
            = tokens[1].substring(6).replaceAll("[\"\\/>]","");
        if (verbose) {
          System.out.println("Found symbol name:" + symbolName);
        }
        List<String> silkFeatures = new ArrayList<String>();
        List<String> attributeFields = new ArrayList<String>();
        pins = new PinList(0); // slots = 0
        while (symbolBundle.hasNext() &&
               !currentLine.startsWith("</symbol")) {
          currentLine = symbolBundle.nextLine().trim();
          if (currentLine.startsWith("<pin")) {
            //System.out.println("#Making new pin: " + currentLine);
            SymbolPin latestPin = new SymbolPin();
            latestPin.populateEagleElement(currentLine);
            pins.addPin(latestPin);
          } else if (currentLine.startsWith("Line") ||
                     currentLine.startsWith("Arc (Layer TOP_SILK")) {
            //silkFeatures.add(currentLine);
          } else if (currentLine.startsWith("Attribute")) {
            //attributeFields.add(currentLine);
          }
        }

        // now we have a list of pins, we can calculate the offsets
        // to justify the element in gschem, and justify the attribute
        // fields.

        // we may need to turn this off if converting entire schematics
        // at some point in the future
        if (!pins.empty()) {
          xOffset = pins.minX();
          yOffset = pins.minY()-200;  // includes bounding box
          // spacing of ~ 200 takes care of the bounding box
          textXOffset = pins.textRHS(); //??? broken for some reason
        }
        // additional bounding box extents are calculated by minY()

        for (String feature : silkFeatures) {
          if (feature.startsWith("Arc (Layer TOP_SILKSCREEN)")) {
            Arc silkArc = new Arc();
            silkArc.populateBXLElement(feature);
            newElement = newElement
                + silkArc.generateGEDAelement(0,-yOffset,1.0f);
          } else if (feature.startsWith("Line")) {
            SymbolPolyline symbolLine = new SymbolPolyline();
            symbolLine.populateBXLElement(feature);
            newElement = newElement
                + "\n" + symbolLine.toString(0,-yOffset);
          } 
        }

        String newSymbolHeader = "v 20110115 1"
            + newElement; // we have created the header for the symbol
        newElement = "";
        String FPField = "";

        // first, we see if there are devicedefs for this symbol
        // mapping its pins onto footprint pads
        //System.out.println("Requesting device defs for " +
        //                   symbolName);
        ArrayList<EagleDevice> symbolDeviceDefs 
            = deviceSets.supplyDevicesFor(symbolName);

        // we have two scenarios, the first is that we have symbols
        // +/- pins defined but no pin mapping for
        // them (= symbolDeviceDef)
        // the second is we have 1 or more pin mappings defined for
        // the symbol found
        if (symbolDeviceDefs.size() == 0) {
          // it seems we have no pin mappings applicable to the symbol
          System.out.println("No matching footprint specified for: "
                             + symbolName);
          attributeFields.add("footprint=unknown");

          SymbolText.resetSymbolTextAttributeOffsets();
          // we now generate the text attribute fields for the current
          // symbol
          for (String attr : attributeFields) {
            symAttributes = symAttributes
                + SymbolText.BXLAttributeString(textXOffset, 0, attr);
          }

          elData = "";
          if (!pins.empty()) { // sometimes Eagle has rubbish symbols
            // with no pins, so we test before we build the symbol
            // note that we did not have a pin mapping we could apply
            // so pin numbers will default to zero
            elData = pins.toString(-xOffset,-yOffset)
                //... header, and then
                + "\n"
                + pins.calculatedBoundingBox(0,0).toString(-xOffset,-yOffset);
          }
            
          // add some attribute fields
          newSymbol = newSymbolHeader + elData + symAttributes;

          // customise symbol filename to reflect applicable FP
          elName = symbolName + ".sym";
          
          // we now write the element to a file
          elementWrite(elName, newSymbol);
          
          // add the symbol to our list of converted elements
          convertedFiles.add(elName);
          
          silkFeatures.clear();
          attributeFields.clear();
          symAttributes = "";
        
        } else { // we get here if >0 symbolDeviceDefs
          // TODO
          // need to generate n symbols for n pin mappings
          // also need to sort out FPName for each variant
          // also need to sort out sane naming convention for
          // the variants of the symbol
          
          for (int index = 0;
               index < symbolDeviceDefs.size();
               index++) {
            if (deviceSets != null &&
                deviceSets.containsSymbol(symbolName) ) {
              //System.out.println("About to renumber pins for "
              //                   + symbolName); 
              if (!pins.empty()) { // sometimes Eagle has odd symbols
                // for fiducials and so forth
                pins.applyEagleDeviceDef(symbolDeviceDefs.get(index));
                textXOffset = pins.textRHS(); // for text justification
              } 
              FPField = symbolDeviceDefs.get(index).supplyFPName();
              attributeFields.add("footprint=" + FPField);
              FPField = "_" + FPField;
            } // start with the first device def to begin with
            
            // when batch converting, we avoid incrementing the
            // justification of text from one symbol to the next, so 
            // we reset the offset variable for each new symbol thusly
            SymbolText.resetSymbolTextAttributeOffsets();
            // we no generate the text attribute fields for the current
            // symbol
            for (String attr : attributeFields) {
              symAttributes = symAttributes
                  + SymbolText.BXLAttributeString(textXOffset, 0, attr);
            }
            
            elData = "";
            if (!pins.empty()) { // sometimes Eagle has rubbish symbols
              // with no pins, so we test before we build the symbol
              elData = pins.toString(-xOffset,-yOffset)
                  //... header, and then
                  + "\n"
                  + pins.calculatedBoundingBox(0,0).toString(-xOffset,-yOffset);
            }
            
            // add some attribute fields
            newSymbol = newSymbolHeader + elData + symAttributes;
            // customise symbol filename to reflect applicable FP
            elName = symbolName + FPField + ".sym";
            
            // we now write the element to a file
            elementWrite(elName, newSymbol);
            
            // add the symbol to our list of converted elements
            convertedFiles.add(elName);
          
            attributeFields.clear();
            symAttributes = "";
          } // end of for loop for pin mappings
          silkFeatures.clear();
        } // end of else statement for >=1 pin mappings

      }
    }
    return convertedFiles.toArray(new String[convertedFiles.size()]);
  } 


  // BSDL files provide pin mapping suitable for symbol generation
  // but do not provide package/footprint information
  private static String [] parseBSDL(String BSDLFile) throws IOException {
    File inputBSDL = new File(BSDLFile);
    Scanner textBSDL = new Scanner(inputBSDL);

    String currentLine = "";
    List<String> portPinDef = new ArrayList<String>();
    String newElement = "";
    String newSymbol = "";
    String symAttributes = "";
    String FPName = "DefaultFPName";
    String elName = null;
    String elData = "";
    PinList pins = new PinList(0); // slots = 0 for BDSL data

    List<String> convertedFiles = new ArrayList<String>();

    long xOffset = 0;
    long yOffset = 0;

    while (textBSDL.hasNext()) {
      currentLine = textBSDL.nextLine().trim();
      if (currentLine.startsWith("entity")) {
        String [] tokens = currentLine.split(" ");
        String symName = tokens[1].replaceAll("[\"]","");
        while (textBSDL.hasNext() &&
               !currentLine.startsWith("end")) {
          currentLine = textBSDL.nextLine().trim();
          if (currentLine.startsWith("constant")) {
            currentLine = currentLine.replaceAll("[:=]"," ");
            tokens = currentLine.split(" ");
            FPName = tokens[1].replaceAll("[\"]","_");
            pins = new PinList(0); // slots = 0
            boolean lastLine = false;
            while (textBSDL.hasNext() &&
                   !lastLine) {
              currentLine = textBSDL.nextLine().trim();
              if (currentLine.length() != 0) {
                SymbolPin latestPin = new SymbolPin();
                latestPin.populateBSDLElement(currentLine);
                pins.addPin(latestPin);
                if (currentLine.endsWith(";")) {
                  lastLine = true;
                }
              }
            }
          } else if (currentLine.startsWith("port (")) {
            boolean endOfPinDef = false;
            while (textBSDL.hasNext() &&
                   !endOfPinDef) {
              currentLine = textBSDL.nextLine().trim();
              if (currentLine.startsWith(")")) {
                endOfPinDef = true;
              } else {
                portPinDef.add(currentLine);
              }
            }
          }
        }
        
        pins.setBSDPinType(portPinDef.toArray(new String[portPinDef.size()]));

        PinList newPinList = pins.createDILSymbol();
        // with a pin list, we can now calculate text label positions
        long textRHSOffset = newPinList.textRHS();
        yOffset = newPinList.minY();// to justify the symbol in gschem 
        // header
        newSymbol = "v 20110115 1";
        // next some attributes
        symAttributes = symAttributes
            + SymbolText.BXLAttributeString(textRHSOffset, 0, "footprint=" + FPName)
            + SymbolText.BXLAttributeString(textRHSOffset, 0, "refdes=U?")
            + SymbolText.BXLAttributeString(textRHSOffset, 0, "documentation=" + BSDLFile);

        // we now build the symbol
        elData = newSymbol   // we now add pins to the...
            + newPinList.toString(xOffset,yOffset)
            //... header, and then
            + "\n"
            + newPinList.calculatedBoundingBox(0,0).toString(0,yOffset)
            + symAttributes;
        elName = symName + ".sym";

        // we now write the element to a file
        elementWrite(elName, elData);
        convertedFiles.add(elName);

        symAttributes = ""; // reset symbol data if batch processing
        // TODO - might be nice to reset BSDL coords in SymbolPinClass
        // if batch converting; probably not essential for usual use
      }
    }
    return convertedFiles.toArray(new String[convertedFiles.size()]);
  }

  // .symdef files provide pin mapping suitable for symbol generation
  // but do not provide package/footprint information
  private static String [] parseSymdef(String symDefFilename) throws IOException {

    File symDefFile = new File(symDefFilename);
    Scanner symDef = new Scanner(symDefFile);

    String currentLine = "";
    String newElement = "";
    String newSymbol = "";
    String symAttributes = "";
    String elData = "";
    String elName = "";

    long xOffset = 0;
    long yOffset = 0; // used to justify symbol
    long textXOffset = 0; // used for attribute fields

    List<String> convertedFiles = new ArrayList<String>();
    List<String> textLabels = new ArrayList<String>();
    List<String> left = new ArrayList<String>();
    List<String> right = new ArrayList<String>();
    List<String> top = new ArrayList<String>();
    List<String> bottom = new ArrayList<String>();

    String currentState = "labels";

    while (symDef.hasNext()) {
      currentLine = symDef.nextLine().trim();
      if (currentLine.startsWith("[labels]") ||
          currentLine.startsWith("[LABELS]")) {
        currentState = "labels";
      } else if (currentLine.startsWith("[left]") ||
                 currentLine.startsWith("[LEFT]")) {
        currentState = "left";
      } else if (currentLine.startsWith("[right]") ||
                 currentLine.startsWith("[RIGHT]")) {
        currentState = "right";
      } else if (currentLine.startsWith("[top]") ||
                 currentLine.startsWith("[TOP]")) {
        currentState = "top";
      } else if (currentLine.startsWith("[bottom]") ||
                 currentLine.startsWith("[BOTTOM]")) {
        currentState = "bottom";
      } else if (currentLine.startsWith(".bus") ||
                 currentLine.startsWith(".BUS")) {
        // don't do anything
      } else if ((currentLine.length() > 1) &&
                 (!currentLine.startsWith("#"))) {
        if (currentState.equals("labels")) {
          if (currentLine.length() > 0) {
            textLabels.add(currentLine);
          }
        } else if (currentState.equals("left")) {
          left.add(currentLine);
        } else if (currentState.equals("bottom")) {
          bottom.add(currentLine);
        } else if (currentState.equals("right")) {
          right.add(currentLine);
        } else if (currentState.equals("top")) {
          top.add(currentLine);
        } 
      }
    }
    PinList pins = new PinList(0); // slots = 0
    for (String line : left) {
      SymbolPin newPin = new SymbolPin();
      newPin.populateSymDefElement(line, "R");
      pins.addPin(newPin);
    }
    for (String line : bottom) {
      SymbolPin newPin = new SymbolPin();
      newPin.populateSymDefElement(line, "U");
      pins.addPin(newPin);
    }
    for (String line : top) {
      SymbolPin newPin = new SymbolPin();
      newPin.populateSymDefElement(line, "D");
      pins.addPin(newPin);
    }
    for (String line : right) {
      SymbolPin newPin = new SymbolPin();
      newPin.populateSymDefElement(line, "L");
      pins.addPin(newPin);
    }

    // our pinsGridAligned method will make the pins nicely spaced
    // around the symbol. 
    PinList newPinList = pins.pinsGridAligned(200);

    // now we have a list of pins, we can calculate the offsets
    // to justify the element in gschem, and justify the attribute
    // fields.
    yOffset = newPinList.minY()-200;  // includes bounding box
    // spacing of ~ 200 takes care of the bounding box

    textXOffset = newPinList.textRHS();
    // additional bounding box extents are calculated by minY()

    for (String attr : textLabels) {
      symAttributes = symAttributes
          + SymbolText.symDefAttributeString(textXOffset, 0, attr);
    }

    newSymbol = "v 20110115 1"
        + newElement; // we have created the header for the symbol
    newElement = "";
    
    // we can now put the pieces of the BXL defined symbol together
    elName = "symDefSymbol.sym";
    elData = newSymbol   // we now add pins to the
        + newPinList.toString(0,-yOffset) // the header, and then
        + "\n" + newPinList.boundingBox(0,0).toString(0,-yOffset)
        + symAttributes; // the final attributes

    // we now write the element to a file
    elementWrite(elName, elData);
    // add the symbol to our list of converted elements
    convertedFiles.add(elName);
    return convertedFiles.toArray(new String[convertedFiles.size()]);
  } 

  // IBIS files provide pin mapping suitable for symbol generation
  // but do not provide package/footprint information
  private static String [] parseIBIS(String IBISFile) throws IOException {
    File input = new File(IBISFile);
    Scanner inputIBIS = new Scanner(input);
    String currentLine = "";
    String newElement = "";
    String newSymbol = "";
    String symAttributes = "";
    String FPName = "DefaultFPName";
    // now we trim the .ibs file ending off:
    String symName = IBISFile.substring(0,IBISFile.length()-4);
    PinList pins = new PinList(0); // slots = 0 for IBIS data

    long xOffset = 0;
    long yOffset = 0;
    boolean extractedSym = false;
    int lineCount = 0;

    while (inputIBIS.hasNext() && !extractedSym) {
      currentLine = inputIBIS.nextLine().trim();
      if (currentLine.startsWith("[Pin]")) {
        while (inputIBIS.hasNext() &&
               (!currentLine.startsWith("[")
                || (lineCount == 0))) {
          currentLine = inputIBIS.nextLine().trim();
          lineCount++;
          if (!currentLine.startsWith("[")) {
            // the pin mapping info ends at the next [] marker
            pins = new PinList(0); // slots = 0
            boolean lastLine = false;
            while (inputIBIS.hasNext() &&
                   !extractedSym) {
              // we make sure it isn't a comment line, i.e. "|" prefix
              if (!currentLine.startsWith("|")) {
                SymbolPin latestPin = new SymbolPin();
                latestPin.populateIBISElement(currentLine);
                pins.addPin(latestPin);
              }
              currentLine = inputIBIS.nextLine().trim();
              if (currentLine.startsWith("[")) {
                extractedSym = true;
              }
            }
          }
        }
      }
    }
    PinList newPinList = pins.createDILSymbol();

    // we can now build the final gschem symbol
    newSymbol = "v 20110115 1";
    String FPAttr = "footprint=" + FPName;
    symAttributes = symAttributes
        + SymbolText.BXLAttributeString(newPinList.textRHS(),0, FPAttr);       
    String elData = newSymbol   // we now add pins to the header...
        + newPinList.toString(xOffset,yOffset)
        // remembering that we built this symbol with coords of
        // our own choosing, i.e. well defined y coords, so don't need
        // to worry about justifying it to display nicely in gschem
        // unlike BXL or similar symbol definitions
        + "\n"
        + newPinList.calculatedBoundingBox(0,0).toString(0,0)
        + symAttributes;
    String elName = symName + ".sym";

    // we now write the element to a file
    elementWrite(elName, elData);
    String [] returnedFilename = {elName};
    return returnedFilename;
  }

  private static void textOnlyBXL(String BXLFile) {
    SourceBuffer buffer = new SourceBuffer(BXLFile); 
    System.out.println(buffer.decode());
  }

  // BXL files provide both pin mapping suitable for symbol
  // generation as well as package/footprint information
  private static String [] parseBXL(String BXLFile) throws IOException {

    SourceBuffer buffer = new SourceBuffer(BXLFile); 
    Scanner textBXL = new Scanner(buffer.decode());

    String currentLine = "";
    String newElement = "";
    String newSymbol = "";
    String symAttributes = "";
    String elData = "";
    String elName = "";
    PadStackList padStacks = new PadStackList();
    PinList pins = new PinList(0); // slots = 0
    List<String> convertedFiles = new ArrayList<String>();

    long xOffset = 0;
    long yOffset = 0; // used to justify symbol
    long textXOffset = 0; // used for attribute fields

    while (textBXL.hasNext()) {
      currentLine = textBXL.nextLine().trim();
      if (currentLine.startsWith("PadStack")) {
          newElement = currentLine;
          while (textBXL.hasNext() &&
                 !currentLine.startsWith("EndPadStack")) {
            currentLine = textBXL.nextLine().trim();
            newElement = newElement + "\n" + currentLine;
          }
          padStacks.addPadStack(newElement);
          newElement = ""; // reset the variable
      } else if (currentLine.startsWith("Pattern ")) {
        String [] tokens = currentLine.split(" ");
        String FPName = tokens[1].replaceAll("[\"]","");
        while (textBXL.hasNext() &&
               !currentLine.startsWith("EndPattern")) {
          currentLine = textBXL.nextLine().trim();
          if (currentLine.startsWith("Pad")) {
            //System.out.println("#Making new pad: " + currentLine);
            Pad newPad = padStacks.GEDAPad(currentLine);
            newElement = newElement
                + newPad.generateGEDAelement(xOffset,yOffset,1.0f);
          } else if (currentLine.startsWith("Line (Layer TOP_SILK")) {
            DrawnElement silkLine = new DrawnElement();
            silkLine.populateBXLElement(currentLine);
            newElement = newElement
                + silkLine.generateGEDAelement(xOffset,yOffset,1.0f);
          } else if (currentLine.startsWith("Arc (Layer TOP_SILK")) {
            Arc silkArc = new Arc();
            silkArc.populateBXLElement(currentLine);
            newElement = newElement
                + silkArc.generateGEDAelement(xOffset,yOffset,1.0f);
          }
        }

        // we now build the geda PCB footprint
        elData = "Element[\"\" \""
            + FPName
            + "\" \"\" \"\" 0 0 0 25000 0 100 \"\"]\n(\n"
            + newElement
            + ")";
        elName = FPName + ".fp";

        // we now write the element to a file
        elementWrite(elName, elData);
        // add the FP to our list of converted elements
        convertedFiles.add(elName); 
        newElement = ""; // reset the variable for batch mode

      } else if (currentLine.startsWith("Symbol ")) {
        String [] tokens = currentLine.split(" ");
        String SymbolName = tokens[1].replaceAll("[\"]","");
        List<String> silkFeatures = new ArrayList<String>();
        List<String> attributeFields = new ArrayList<String>();
        pins = new PinList(0); // slots = 0
        while (textBXL.hasNext() &&
               !currentLine.startsWith("EndSymbol")) {
          currentLine = textBXL.nextLine().trim();
          if (currentLine.startsWith("Pin")) {
            //System.out.println("#Making new pin: " + currentLine);
            SymbolPin latestPin = new SymbolPin();
            currentLine = currentLine + " " +
                textBXL.nextLine().trim() + " " +
                textBXL.nextLine().trim(); // we combine the 3 lines
            latestPin.populateBXLElement(currentLine);
            pins.addPin(latestPin);
          } else if (currentLine.startsWith("Line") ||
                     currentLine.startsWith("Arc (Layer TOP_SILK")) {
            silkFeatures.add(currentLine);
          } else if (currentLine.startsWith("Attribute")) {
            attributeFields.add(currentLine);
          }
        }

        // now we have a list of pins, we can calculate the offsets
        // to justify the element in gschem, and justify the attribute
        // fields.
        yOffset = pins.minY()-200;  // includes bounding box
        // spacing of ~ 200 takes care of the bounding box
        textXOffset = pins.textRHS();
        // additional bounding box extents are calculated by minY()

        for (String feature : silkFeatures) {
          if (feature.startsWith("Arc (Layer TOP_SILKSCREEN)")) {
            Arc silkArc = new Arc();
            silkArc.populateBXLElement(feature);
            newElement = newElement
                + silkArc.generateGEDAelement(0,-yOffset,1.0f);
          } else if (feature.startsWith("Line")) {
            SymbolPolyline symbolLine = new SymbolPolyline();
            symbolLine.populateBXLElement(feature);
            newElement = newElement
                + "\n" + symbolLine.toString(0,-yOffset);
          } 
        }

        for (String attr : attributeFields) {
          symAttributes = symAttributes
              + SymbolText.BXLAttributeString(textXOffset, 0, attr);
        }

        newSymbol = "v 20110115 1"
            + newElement; // we have created the header for the symbol
        newElement = "";
        silkFeatures.clear();
        attributeFields.clear();
        
      } else if (currentLine.startsWith("Component ")) {
        // we now parse the other attributes for the component
        String [] tokens = currentLine.split(" ");
        String symbolName = tokens[1].replaceAll("[\"]","");
        while (textBXL.hasNext() &&
               !currentLine.startsWith("EndComponent")) {
          currentLine = textBXL.nextLine().trim();
          if (currentLine.startsWith("Attribute")) {
            //SymbolText attrText = new SymbolText();
            //attrText.populateBXLElement(currentLine);
            symAttributes = symAttributes
                + SymbolText.BXLAttributeString(textXOffset, 0, currentLine);
          } else if (currentLine.startsWith("RefDesPrefix")) {
            currentLine = currentLine.replaceAll(" ", "");
            currentLine = currentLine.split("\"")[1];
            String refDesAttr = "refdes=" + currentLine + "?";
            symAttributes = symAttributes
                  + SymbolText.BXLAttributeString(textXOffset, 0, refDesAttr);
          } else if (currentLine.startsWith("PatternName")) {
            currentLine = currentLine.replaceAll(" ", "");
            currentLine = currentLine.split("\"")[1];
            String FPAttr = "footprint=" + currentLine;
            symAttributes = symAttributes
                  + SymbolText.BXLAttributeString(textXOffset, 0, FPAttr);
          } else if (currentLine.startsWith("AlternatePattern")) {
            currentLine = currentLine.replaceAll(" ", "");
            currentLine = currentLine.split("\"")[1];
            String AltFPAttr = "alt-footprint=" + currentLine;
            symAttributes = symAttributes
                  + SymbolText.BXLAttributeString(textXOffset, 0, AltFPAttr);
          } else if (currentLine.startsWith("CompPin ")) {
            pins.setBXLPinType(currentLine);
          }
        }

        // we can now put the pieces of the BXL defined symbol together
        elName = symbolName + ".sym";
        elData = newSymbol   // we now add pins to the
            + pins.toString(0,-yOffset) // the header, and then
            + symAttributes; // the final attributes

        // we now write the element to a file
        elementWrite(elName, elData);
        // add the symbol to our list of converted elements
        convertedFiles.add(elName);
        // and we rest the variable for the next symbol
        symAttributes = "";
      }
    }
    return convertedFiles.toArray(new String[convertedFiles.size()]);
  } 


  public static void elementWrite(String elementName,
                                  String data) throws IOException {
    try {
      File newElement = new File(elementName);
      PrintWriter elementOutput = new PrintWriter(newElement);
      elementOutput.println(data);
      elementOutput.close();
    } catch(Exception e) {
      System.out.println("There was an error saving: "
                         + elementName); 
      System.out.println(e);
    }
  }

  public static void printHelp() {
    System.out.println("usage:\n\n\tjava BSDL2GEDA BSDLFILE.bsd\n\n"
                       + "options:\n\n"
                       + "\t\t-t\tonly output converted text"
                       + " without further conversion\n\n"
                       + "example:\n\n"
                       + "\tjava BSDL2GEDA BSDLFILE.bsd"
                       + " -t > BSDLFILE.txt\n");

  }

  private static void defaultFileIOError(Exception e) {
        System.out.println("Hmm, that didn't work. "
                           + "Probably a file IO issue:");
        System.out.println(e);
  }
  
}

