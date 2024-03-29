const SIZES = {
    grid: {
        w: 600,
        h: 600
    },
    dataPoint: {
        min: 10,
        max: 50,
        default: 20
    }
};
const COLORS = {
    scale: d3.interpolateRgb("#F55", "#8F8"),
    default: "#DDD"
};

const VIEW_CRD = {
  x : {},
  y : {},
  size : {},
  color : {}
};

let config;
let space;
const dataPoints = {};
let repoModal;
const nodes = [];
let id2Title;
let simulation;
let longestCrd;

let rawData;

// Simulation data
let dataPointArr; 

// SVG <g> elements for each data point
let dataPointGs;
const axesLines = {
    h:[],
    v:[]
};

const dim2coords = {};
const vizEmts = {
    dataPoints: d3.select("#dataPoints"),
    gridH: d3.select("#grid-h"),
    gridV: d3.select("#grid-v"),
    sizeEmtSvg: document.getElementById("sizeEmtTemplate")
};
let initComplete = false;

const menus = {
    x:     { emt: document.getElementById("selectX"),     value:"" },
    y:     { emt: document.getElementById("selectY"),     value:"" },
    color: { emt: document.getElementById("selectColor"), value:"" },
    size:  { emt: document.getElementById("selectSize"),  value:"" }
}

const BY_ID = d=>d.id;

function cc2title( text ) {
    let retVal = text.replaceAll(/([A-Z])/g, " $1").trim();
    while ( retVal.match(/[A-Z] [A-Z]/) ) {
        retVal = retVal.replaceAll(/([A-Z]) ([A-Z])/g, "$1$2");
    }

    return retVal;
}

function lastSlotName( coordinateName ) {
    let arr = coordinateName.split("/");
    return arr[arr.length-1];
}

function loadData() {
    return d3.json("data/repoNames.json").then( id2t => {
        id2Title = id2t;
    }).then( _gi => d3.json("data/summary.json")
    ).then(function(data){
        rawData = data;
        space = data.space;
        const menuEmts = {};
        Object.keys(menus).forEach( k => menuEmts[k]=menus[k] );
        buildDimensionSelects(space, [], 0);
        longestCrd = Object.values(dim2coords).map(a=>a.length).reduce( (x,y)=>x>y?x:y );
        const tsptIds = Object.keys(data.transcripts);
        tsptIds.forEach( id => {
            dataPoints[id] = data.transcripts[id];
            dataPoints[id].id = id;
            const node = {
                data: dataPoints[id],
                x:(Math.random()*SIZES.grid.w),
                y:SIZES.grid.h
            };
            nodes.push( node );
        });
    });
}

function makeCoordinateGetter( crdStr ) {
    const crd = crdStr.split("/");
    return function( value ) {
        for ( let x=0; x<crd.length; x++ ) {
            if ( crd[x] && value[crd[x]] ) {
                value = value[crd[x]];
            } else {
                return -1000;
            }
        }
        return value;
    }
}

function mockValue( crdStr, value ) {
    const retVal = {};
    let cur = retVal;
    const arr = crdStr.split("/").reverse();
    
    while ( arr.length > 0 ){
        const crd = arr.pop();
        cur[crd] = (arr.length>0) ? {} : value;
        cur = cur[crd];
    }

    return retVal;
    
}

function makeScreenLocationForce( pomoCrdStr, maxPixel, reversed ) {
    const pomoGetter = makeCoordinateGetter(pomoCrdStr);
    const pomoValues = dim2coords[pomoCrdStr];
    const bandWidth = maxPixel / pomoValues.length;
    const halfBandWidth = bandWidth/2;
    return function(d){
        const pomoCrdVal = pomoGetter(d.data);
        const pomoCrdIdx = Math.max(0,pomoValues.indexOf(pomoCrdVal));
        const screenLocation = bandWidth*pomoCrdIdx + halfBandWidth;
        return reversed ? maxPixel-screenLocation : screenLocation;
    };
}

function makeDataPointSizeFunction( pomoCrdStr ) {
    const pomoGetter = makeCoordinateGetter(pomoCrdStr);
    const pomoValues = dim2coords[pomoCrdStr];
    const sizeFactor = (SIZES.dataPoint.max - SIZES.dataPoint.min) / pomoValues.length;
    return function(d){
        const pomoCrdVal = pomoGetter(d.data);
        const pomoCrdIdx = Math.max(0,pomoValues.indexOf(pomoCrdVal));
        return SIZES.dataPoint.min + pomoCrdIdx * sizeFactor;
    };
}

function menuSelectionChanged( aKey ) {
    if ( ! initComplete ) return;
    readMenuStates(aKey);
    
    if ( aKey === "color" ) {
        dataPointGs.selectAll("circle").transition().duration(500).style("fill",dataPointColor);
        if ( menus.color.value==="" ) {
            $("#colorScale").hide();
        } else {
            updateColorScale();
            $("#colorScale").show();
        }
        return; // no need to move any point.
    }

    if ( aKey === "size" ) {
        dataPointGs.selectAll("circle").transition().duration(500).attr("r",dataPointSize);
        if ( menus.size.value==="" ) {
            $("#sizeScale").hide();
        } else {
            updateSizeScale();
            $("#sizeScale").show();
        }
    }
    if ( simulation ) {
        simulation.stop();
    }
    createSimulation();
    if ( aKey === "y" ) {
        updateHTicks();
        updateAxis(aKey);
    } else if ( aKey ==="x" ) {
        updateVTicks();
        updateAxis(aKey);
    }
}

function readMenuStates(aKey) {

    // Forces
    VIEW_CRD.x.values = dim2coords[menus.x.value];
    VIEW_CRD.x.force = makeScreenLocationForce(menus.x.value, SIZES.grid.w, false);
    
    VIEW_CRD.y.values = dim2coords[menus.y.value];
    VIEW_CRD.y.force = makeScreenLocationForce(menus.y.value, SIZES.grid.h, true);
    
    // Visual properties
    VIEW_CRD.color.values = (menus.color.value!=="") ? dim2coords[menus.color.value] : [];
    VIEW_CRD.color.crdStr = (menus.color.value!=="") ? menus.color.value : null;
    VIEW_CRD.color.getCrd = (menus.color.value!=="") ? makeCoordinateGetter(menus.color.value) : null;
    
    VIEW_CRD.size.values = (menus.size.value!=="") ? dim2coords[menus.size.value] : [];
    VIEW_CRD.size.crdStr = (menus.size.value!=="") ? menus.size.value : null;
    VIEW_CRD.size.force = (menus.size.value!=="") ? makeDataPointSizeFunction(menus.size.value) : null;

}

function updateAxis(axisName) {
    let crdText = menus[axisName].value;
    if ( ! crdText ) return; //still in setup.
    crdText = crdText.split("/");
    crdText = crdText[crdText.length-1];
    document.getElementById(axisName+"Label").innerText = cc2title(crdText);
    const crdEmt = document.getElementById(axisName + "Crds");
    Utils.clear(crdEmt);
    let crdValues = dim2coords[menus[axisName].value];
    if ( crdValues ) {
        if ( axisName === "y" ) {
            crdValues = crdValues.slice();
            crdValues.reverse();
        }
        crdValues.forEach( v => {
            const emt = document.createElement("li");
            emt.innerText = v.replaceAll(/([A-Z])/g, " $1");
            crdEmt.append(emt);
        });
    }
}

function updateColorScale() {
    const colScEmt = document.getElementById("colorScale").getElementsByTagName("ul")[0];
    const crdText =lastSlotName(menus.color.value);
    
    Utils.clear(colScEmt);
    let li = document.createElement("li");
    li.classList.add("title")
    li.textContent=cc2title(crdText);
    colScEmt.appendChild(li);

    VIEW_CRD.color.values.forEach( v => {
        li = document.createElement("li");
        li.textContent = cc2title(v);
        li.style.backgroundColor = dataPointColor({data:mockValue(VIEW_CRD.color.crdStr, v)});
        colScEmt.appendChild(li);
    });
}

function updateSizeScale() {
    const szScEmt = document.getElementById("sizeScale").getElementsByTagName("ul")[0];
    const crdText = lastSlotName(menus.size.value);
    
    Utils.clear(szScEmt);
    let li = document.createElement("li");
    li.classList.add("title")
    li.textContent=cc2title(crdText);
    szScEmt.appendChild(li);

    const sizeFactor = (SIZES.dataPoint.max - SIZES.dataPoint.min) / VIEW_CRD.size.values.length;
    
    VIEW_CRD.size.values.forEach( t => {
        li = document.createElement("li");
        const sizeEmt = vizEmts.sizeEmtSvg.cloneNode(true);
        const r = VIEW_CRD.size.force( {data:mockValue(VIEW_CRD.size.crdStr,t)});
        const crc = sizeEmt.getElementsByTagName("circle")[0];
        crc.setAttribute("r", r );
        crc.setAttribute("cx", SIZES.dataPoint.max );
        crc.setAttribute("cy", r );
        sizeEmt.style.width= (SIZES.dataPoint.max*2)+"px";
        sizeEmt.style.height= (r*2)+"px";
        li.appendChild(sizeEmt);
        const textEmt = document.createElement("div");
        textEmt.textContent=cc2title(t);
        li.appendChild( textEmt );
        szScEmt.appendChild( li );
    });
}

function buildDimensionSelects(slot, slotNameStack, level) {
    const optionText = slotNameStack[slotNameStack.length-1];
    if ( slot.length ) {
        // Array - we've reached an actual dimension
        const optionValue = slotNameStack.join("/");
        dim2coords[optionValue] = slot;
        
        Object.keys(menus).forEach(k =>{
            const emt = document.createElement("a");
            emt.classList.add("dropdown-item");
            emt.dataset.value=optionValue;
            emt.dataset.level=level;
            emt.innerText = cc2title(optionText);
            menus[k].emt.getElementsByClassName("hs-menu-inner")[0].appendChild(emt);
        });
        
    } else {
        // Object - expand the dimensions recursively
        if ( optionText ) {
            Object.keys(menus).forEach(k =>{
                const emt = document.createElement("a");
                emt.classList.add("dropdown-item");
                emt.dataset.value="";
                emt.dataset.level=level;
                emt.ariaDisabled=true;
                emt.classList.add("disabled");
                emt.disabled="disabled";
                emt.innerText = cc2title(optionText);
                menus[k].emt.getElementsByClassName("hs-menu-inner")[0].appendChild(emt);
            });
        }
        
        Object.keys(slot).forEach( k => {
            slotNameStack.push(k);
            buildDimensionSelects( slot[k], slotNameStack, level+1 );
            slotNameStack.pop();
        });
    }
}

function drawGraph(){
    addDataPoints();
}

function updateHTicks() {
    const bandWidth = SIZES.grid.h/VIEW_CRD.y.values.length;
    const scaler = d => bandWidth*(d+1);
    const lines = vizEmts.gridH.selectAll("line")
        .data( d3.range(0,VIEW_CRD.y.values.length-1) );
    lines.enter()
        .append("line")
        .attr("x1",0)
        .attr("x2",SIZES.grid.w)
        .attr("y1", scaler )
        .attr("y2", scaler )
        .style("opacity",0)
        .transition().duration(500).style("opacity",1);
    
    lines.transition().duration(500).attr("y1", scaler )
            .attr("y2", scaler );

    lines.exit().transition().duration(500).style("opacity",0).transition().remove();
}

function updateVTicks() {
    const bandWidth = SIZES.grid.w/VIEW_CRD.x.values.length;
    const scaler = d => bandWidth*(d+1);
    const lines = vizEmts.gridV.selectAll("line")
        .data( d3.range(0,VIEW_CRD.x.values.length-1) );
    lines.enter()
        .append("line")
        .attr("x1", scaler )
        .attr("x2", scaler )
        .attr("y1",0)
        .attr("y2",SIZES.grid.h)
        .style("opacity",0)
        .transition().duration(500).style("opacity",1);
    
    lines.transition().duration(500).attr("x1", scaler )
            .attr("x2", scaler );

    lines.exit().transition().duration(500).style("opacity",0).transition().remove();
}


function addDataPoints() {
    dataPointGs = vizEmts.dataPoints.selectAll("g")
        .data( nodes, BY_ID )
        .enter()
        .append("g")
        .attr("id", BY_ID)
        .attr("class", "repo")
        .attr("transform", d=>"translate(" + d.x + "," + d.y + ")")
        .on("mousedown", (e,d)=>showData(d.data.id))
        .on("mouseover", (e,d)=>e.target.classList.add("hovered"))
        .on("mouseout",  (e,d)=>e.target.classList.remove("hovered"));

    dataPointGs.append("circle").attr("cx", 0).attr("cy", 0)
                .attr("r", dataPointSize).style("fill", COLORS.default );
    dataPointGs.append("text").attr("x", 0).attr("y", 0)
            .attr("text-anchor","middle").attr("dy","0.35em")
            .text( d=>resilientTitle(d, "id") );
    
}

function resilientTitle( dataPoint, mode ) {
    const id = dataPoint.data.id;
    const repoNameData = id2Title[id];
    if ( repoNameData && repoNameData[mode] ) {
        return repoNameData[mode];
    } else {
        console.log("Missing "+mode+" for %o", dataPoint );
        return id;
    }
}

function dataPointSize(r){
    return (VIEW_CRD.size.force) ? VIEW_CRD.size.force(r) : SIZES.dataPoint.default;
}

function dataPointColor(r) {
    if ( VIEW_CRD.color.getCrd ) {
        const idx = Math.max(0, VIEW_CRD.color.values.indexOf(VIEW_CRD.color.getCrd(r.data)));
        return COLORS.scale( idx/VIEW_CRD.color.values.length );
    } else {
        return COLORS.default;
    }
}

function createSimulation() {
    simulation = d3.forceSimulation(nodes)
    .force("charge", d3.forceCollide(d => dataPointSize(d) + 5))
    .force("forceX", d3.forceX(VIEW_CRD.x.force))
    .force("forceY", d3.forceY(VIEW_CRD.y.force))
    .on("tick", ticked);
}

function ticked() {
    dataPointGs.transition().ease(d3.easeLinear).duration(300)
            .attr('transform', (d) => "translate(" + d.x + "," 
                                                   + d.y + ")");
}


function start() {
    SIZES.grid.cx = (SIZES.grid.w/2);
    SIZES.grid.cy = (SIZES.grid.h/2);
    COLORS.default = COLORS.scale(0.5);
    vizEmts.sizeEmtSvg.remove();
    vizEmts.sizeEmtSvg.removeAttribute("id");

    d3.json( "config.json" )
    .then( c => {
        config=c;
        document.getElementById("config-title").innerHTML=config.title;
        document.getElementById("config-subtitle").innerHTML=config.subtitle;
        document.getElementById("config-footer").innerHTML=config.footer;

        return loadData();

    }).then( ()=>{
        Object.keys(menus).forEach(k =>{
            const id = menus[k].emt.id;
            const defaultValue = config.axes[k];
            if ( defaultValue.length > 0 ) {
                menus[k].emt.getElementsByClassName("d-none")[0].value = defaultValue;
            }

            $("#"+id).hierarchySelect({
                width: 'auto',
                initialValueSet: true,
                search: false,
                onChange: function (value) {
                    menus[k].value = value;
                    if ( initComplete ) {
                        menuSelectionChanged(k);
                    }
                }
            }); 
        });
        drawGraph();

    }).then(()=>{
        menus.x.value = config.axes.x;
        menus.y.value = config.axes.y;
        updateAxis("x");
        updateAxis("y");
        readMenuStates();
        updateHTicks();
        updateVTicks();
        initComplete = true;
        createSimulation();
    });
}

let isTitlesShown=true;

function toggleTitles() {
    const opc = isTitlesShown ? 0 : 1;
    $("g text").css("opacity", opc);

    isTitlesShown = !isTitlesShown;
}

function updateDatapointTitle(modeName) {
    switch (modeName) {
        case "none":
            vizEmts.dataPoints.selectAll("g").data(nodes, n=>n.data.id).select("text").text("");
            break;
        case "title":
            vizEmts.dataPoints.selectAll("g").data(nodes, n=>n.data.id).select("text").text(d=>resilientTitle(d,"title"));
            break;
        case "id":
            vizEmts.dataPoints.selectAll("g").data(nodes, n=>n.data.id).select("text").text(d=>resilientTitle(d,"id"));
            break;
    }
}

function showData( id ) {
    if ( ! repoModal ) {
        repoModal = new bootstrap.Modal(document.getElementById('repoModal'));
    }
    const info = id2Title[id];
    document.getElementById("repoIdField").textContent = info.id;
    document.getElementById("repoTitleField").textContent = info.title;
    repoModal.show();
}