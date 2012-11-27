Storage 
{
	{ Format 1.33 }
	{ GeneratedFrom TGD-version-2.20 }
	{ WrittenBy imiller }
	{ WrittenOn "" }
}

Document 
{
	{ Type "Generic Diagram" }
	{ Name Constants.gd }
	{ Author imiller }
	{ CreatedOn "" }
	{ Annotation "" }
	{ Hierarchy False }
}

Page 
{
	{ PageOrientation Landscape }
	{ PageSize Letter }
	{ ShowHeaders False }
	{ ShowFooters False }
	{ ShowNumbers False }
}

Scale 
{
	{ ScaleValue 1 }
}

# GRAPH NODES

GenericNode 1
{
	{ Name "Constant\risLocked() : boolean\rgetValueBus() : Bus\rgetRep () : ByteRep[]\rgetConstituents () : List(Constant)\rgetContents () : Set (Constant)" }
	{ Annotation "" }
	{ Parent 0 }
	{ Index "" }
}

GenericNode 2
{
	{ Name "MemConstant\risBigEndian() : boolean\rpushValuesForward () : boolean\rpushValuesBackward () : boolean\rswapEndian (ByteRep[]) : ByteRep[]" }
	{ Annotation "" }
	{ Parent 0 }
	{ Index "" }
}

GenericNode 3
{
	{ Name "AggregateConstant" }
	{ Annotation "" }
	{ Parent 0 }
	{ Index "" }
}

GenericNode 4
{
	{ Name "LocationConstant" }
	{ Annotation "" }
	{ Parent 0 }
	{ Index "" }
}

GenericNode 5
{
	{ Name "SliceConstant" }
	{ Annotation "" }
	{ Parent 0 }
	{ Index "" }
}

EmptyNode 6
{
	{ Name "" }
	{ Annotation "" }
	{ Parent 0 }
	{ Index "" }
}

GenericNode 28
{
	{ Name "ScalarConstant" }
	{ Annotation "" }
	{ Parent 0 }
	{ Index "" }
}

Comment 32
{
	{ Name "getRep is always in 'endian order', ie\ras the value was/would be represented\rin memory.\r\rThe Value on the valueBus is always\rin little endian representation" }
	{ Annotation "" }
	{ Parent 0 }
	{ Index "" }
}

GenericNode 33
{
	{ Name "ByteRep\rvalue () : byte\rlocked () : boolean" }
	{ Annotation "" }
	{ Parent 0 }
	{ Index "" }
}

GenericNode 38
{
	{ Name "SimpleConstant" }
	{ Annotation "" }
	{ Parent 0 }
	{ Index "" }
}

EmptyNode 39
{
	{ Name "" }
	{ Annotation "" }
	{ Parent 0 }
	{ Index "" }
}

Comment 40
{
	{ Name "getConstituents returns, in order, all \rthe Constants that are used to generate\rthe value of the constant, ie all Scalar\rSlice, and Location Constants in all\rrecords, unnested and flattened\r\rgetContents returns a Set view of all\rConstants used to make up another constant\rthis means that both a Slice and its \rbacking constant will appear in the set\ras well as an Aggregate and all its \rcomponent constants" }
	{ Annotation "" }
	{ Parent 0 }
	{ Index "" }
}

# GRAPH EDGES

GenericEdge 8
{
	{ Name "" }
	{ Annotation "" }
	{ Parent 0 }
	{ Subject1 3 }
	{ Subject2 6 }
}

GenericEdge 9
{
	{ Name "" }
	{ Annotation "" }
	{ Parent 0 }
	{ Subject1 4 }
	{ Subject2 6 }
}

GenericEdge 10
{
	{ Name "" }
	{ Annotation "" }
	{ Parent 0 }
	{ Subject1 5 }
	{ Subject2 6 }
}

GenericEdge 11
{
	{ Name "" }
	{ Annotation "" }
	{ Parent 0 }
	{ Subject1 6 }
	{ Subject2 2 }
}

GenericEdge 12
{
	{ Name "[1..N]\relements\r{0..N}" }
	{ Annotation "" }
	{ Parent 0 }
	{ Subject1 3 }
	{ Subject2 2 }
}

GenericEdge 29
{
	{ Name "" }
	{ Annotation "" }
	{ Parent 0 }
	{ Subject1 28 }
	{ Subject2 6 }
}

GenericEdge 41
{
	{ Name "" }
	{ Annotation "" }
	{ Parent 0 }
	{ Subject1 2 }
	{ Subject2 39 }
}

GenericEdge 42
{
	{ Name "" }
	{ Annotation "" }
	{ Parent 0 }
	{ Subject1 38 }
	{ Subject2 39 }
}

GenericEdge 43
{
	{ Name "" }
	{ Annotation "" }
	{ Parent 0 }
	{ Subject1 39 }
	{ Subject2 1 }
}

GenericEdge 50
{
	{ Name "[1]\rbyteValues\r{1..N}" }
	{ Annotation "" }
	{ Parent 0 }
	{ Subject1 33 }
	{ Subject2 1 }
}

GenericEdge 51
{
	{ Name "[1]\rbyteValues\r{1..N}" }
	{ Annotation "" }
	{ Parent 0 }
	{ Subject1 5 }
	{ Subject2 2 }
}

# VIEWS AND GRAPHICAL SHAPES

View 14
{
	{ Index "0" }
	{ Parent 0 }
}

Box 15
{
	{ View 14 }
	{ Subject 1 }
	{ Position 310 60 }
	{ Size 292 88 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FillStyle Unfilled }
	{ FillColor "white" }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Box 16
{
	{ View 14 }
	{ Subject 2 }
	{ Position 500 230 }
	{ Size 282 86 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FillStyle Unfilled }
	{ FillColor "white" }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Box 17
{
	{ View 14 }
	{ Subject 3 }
	{ Position 260 390 }
	{ Size 140 40 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FillStyle Unfilled }
	{ FillColor "white" }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Box 18
{
	{ View 14 }
	{ Subject 4 }
	{ Position 420 390 }
	{ Size 132 40 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FillStyle Unfilled }
	{ FillColor "white" }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Box 19
{
	{ View 14 }
	{ Subject 5 }
	{ Position 720 390 }
	{ Size 108 40 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FillStyle Unfilled }
	{ FillColor "white" }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

BlackDot 21
{
	{ View 14 }
	{ Subject 6 }
	{ Position 500 320 }
	{ Size 8 8 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FillStyle Unfilled }
	{ FillColor "white" }
	{ FixedName True }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Line 22
{
	{ View 14 }
	{ Subject 8 }
	{ FromShape 17 }
	{ ToShape 21 }
	{ Curved False }
	{ End1 Empty }
	{ End2 Empty }
	{ Points 2 }
	{ Point 329 370 }
	{ Point 496 321 }
	{ NamePosition 409 336 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Line 23
{
	{ View 14 }
	{ Subject 9 }
	{ FromShape 18 }
	{ ToShape 21 }
	{ Curved False }
	{ End1 Empty }
	{ End2 Empty }
	{ Points 2 }
	{ Point 443 370 }
	{ Point 497 323 }
	{ NamePosition 461 339 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Line 24
{
	{ View 14 }
	{ Subject 10 }
	{ FromShape 19 }
	{ ToShape 21 }
	{ Curved False }
	{ End1 Empty }
	{ End2 Empty }
	{ Points 2 }
	{ Point 690 370 }
	{ Point 504 321 }
	{ NamePosition 600 336 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Line 25
{
	{ View 14 }
	{ Subject 11 }
	{ FromShape 21 }
	{ ToShape 16 }
	{ Curved False }
	{ End1 Empty }
	{ End2 FilledArrow }
	{ Points 2 }
	{ Point 500 316 }
	{ Point 500 273 }
	{ NamePosition 486 294 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Line 26
{
	{ View 14 }
	{ Subject 12 }
	{ FromShape 17 }
	{ ToShape 16 }
	{ Curved False }
	{ End1 Empty }
	{ End2 OpenArrow }
	{ Points 3 }
	{ Point 250 370 }
	{ Point 250 230 }
	{ Point 359 230 }
	{ NamePosition 295 284 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Box 30
{
	{ View 14 }
	{ Subject 28 }
	{ Position 570 390 }
	{ Size 116 40 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FillStyle Unfilled }
	{ FillColor "white" }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Line 31
{
	{ View 14 }
	{ Subject 29 }
	{ FromShape 30 }
	{ ToShape 21 }
	{ Curved False }
	{ End1 Empty }
	{ End2 Empty }
	{ Points 2 }
	{ Point 550 370 }
	{ Point 503 323 }
	{ NamePosition 535 339 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

TextBox 35
{
	{ View 14 }
	{ Subject 32 }
	{ Position 180 480 }
	{ Size 20 20 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FillStyle Unfilled }
	{ FillColor "white" }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Left }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Box 36
{
	{ View 14 }
	{ Subject 33 }
	{ Position 620 120 }
	{ Size 156 46 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FillStyle Unfilled }
	{ FillColor "white" }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Box 44
{
	{ View 14 }
	{ Subject 38 }
	{ Position 150 210 }
	{ Size 116 40 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FillStyle Unfilled }
	{ FillColor "white" }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

BlackDot 45
{
	{ View 14 }
	{ Subject 39 }
	{ Position 310 140 }
	{ Size 8 8 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FillStyle Unfilled }
	{ FillColor "white" }
	{ FixedName True }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Line 46
{
	{ View 14 }
	{ Subject 41 }
	{ FromShape 16 }
	{ ToShape 45 }
	{ Curved False }
	{ End1 Empty }
	{ End2 Empty }
	{ Points 2 }
	{ Point 409 187 }
	{ Point 314 142 }
	{ NamePosition 366 155 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Line 47
{
	{ View 14 }
	{ Subject 42 }
	{ FromShape 44 }
	{ ToShape 45 }
	{ Curved False }
	{ End1 Empty }
	{ End2 Empty }
	{ Points 2 }
	{ Point 196 190 }
	{ Point 306 142 }
	{ NamePosition 246 157 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Line 48
{
	{ View 14 }
	{ Subject 43 }
	{ FromShape 45 }
	{ ToShape 15 }
	{ Curved False }
	{ End1 Empty }
	{ End2 FilledArrow }
	{ Points 2 }
	{ Point 310 136 }
	{ Point 310 104 }
	{ NamePosition 296 120 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

TextBox 49
{
	{ View 14 }
	{ Subject 40 }
	{ Position 530 520 }
	{ Size 20 20 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FillStyle Unfilled }
	{ FillColor "white" }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Left }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Line 52
{
	{ View 14 }
	{ Subject 50 }
	{ FromShape 36 }
	{ ToShape 15 }
	{ Curved False }
	{ End1 Empty }
	{ End2 Empty }
	{ Points 3 }
	{ Point 610 97 }
	{ Point 610 40 }
	{ Point 456 40 }
	{ NamePosition 565 68 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

Line 53
{
	{ View 14 }
	{ Subject 51 }
	{ FromShape 19 }
	{ ToShape 16 }
	{ Curved False }
	{ End1 Empty }
	{ End2 OpenArrow }
	{ Points 3 }
	{ Point 750 370 }
	{ Point 750 230 }
	{ Point 641 230 }
	{ NamePosition 706 290 }
	{ Color "black" }
	{ LineWidth 1 }
	{ LineStyle Solid }
	{ FixedName False }
	{ Font "-*-courier-medium-r-normal--10*" }
	{ TextAlignment Center }
	{ TextColor "black" }
	{ NameUnderlined False }
}

