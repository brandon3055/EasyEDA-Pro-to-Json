# EasyEDA-Pro-to-Json

This is a java application that converts EasyEDA Pro project files to the json format used by [openscopeproject/InteractiveHtmlBom](https://github.com/openscopeproject/InteractiveHtmlBom)

Please be aware this is far from a perfect conversion. Some things are a little broken like polygon pads and some component silkscreen elements, Some elements are missing like all silkscreen text.
I pretty much just got this to the point where it produces a *usable* IBOM and left it there. 
I may continue to develop this in the future but for now it will stay as is.
If anyone else wants to expand on this then be my guest.

### Usage
1. Open the PCB in the editor then download PCB document using `File>Document Save As (Local)` The resulting zip contains everything needed to generate the IBOM.
You can also `File>Project Save As (Local)` will also work if your project has a single board.
It is no longer possible to use `File>File Source` as this no longer contains all the required data to generate the ibom. 
![]( https://ss.brandon3055.com/af380)

2. Next process the downloaded zip using ether `java -jar EDAProToIBOM.jar Project/Document-Export.zip` or `java -jar EDAProToIBOM.jar Project/Document-Export.zip output_file.json`
The resulting json can then be fed into [openscopeproject/InteractiveHtmlBom](https://github.com/openscopeproject/InteractiveHtmlBom)
Be aware the converter requires java 17 or higher.

At the time of writing this i only have a couple EasyEDA Pro projects but they both convert to usable iboms.
![](https://ss.brandon3055.com/0ca33) 
![](https://ss.brandon3055.com/a4646)
