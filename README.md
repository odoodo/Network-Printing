# Network-Printing
A small Java library that allows to print documents of type
* PDF
* HTML
* Text

from within Mirth on the fly to a network printer w/o having a driver installed locally.

# Functionality
Print a pdf document represented by a byte array:
```js
Packages.lu.hrs.mirth.NetworkPrinting.printPdf(documentToPrintAsByteArray, printerIpOrName);
```
Print a base64-encoded document:
```js
Packages.lu.hrs.mirth.NetworkPrinting.printPdfBase64(base64EncodedDocumentToPrint, printerIpOrName);
```
Print an HTML page:
```js
Packages.lu.hrs.mirth.NetworkPrinting.printHtml(htmlCode, printerIpOrName);
```
Print text:
```js
Packages.lu.hrs.mirth.NetworkPrinting.printText(textToPrint, printerIpOrName, encoding);
```
# Installation
1. unpack the [latest release](https://github.com/odoodo/Network-Printing/releases)
2. place the whole folder in either the custom-lib folder or (better) place the folder in your Mirth installation and add a specific resource "NetworkPrinting" 
3. reload resources (assure that _Include all Subdirectories_-checkbox is checked)
4. reference the custom resource in your channel

