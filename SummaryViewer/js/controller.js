const SIZES = {
    grid: {
        w: 600,
        h: 600
    },
    dataPoint: {
        min: 20,
        max: 60,
        default: 30
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

const spaces="                ";
let space;
const dataPoints ={};
const nodes = [];
let id2Title;
let simulation;

// Simulation data
let dataPointArr; 

// SVG <g> elements for each data point
let dataPointGs;

const dim2coords = {};
const vizEmts = {
    dataPoints: d3.select("#dataPoints")
};
let initComplete = false;

const menus = {
    x: document.getElementById("sltX"),
    y: document.getElementById("sltY"),
    color: document.getElementById("sltColor"),
    size: document.getElementById("sltSize")
}

const BY_ID = d=>d.id;

function loadData() {
    return d3.json("data/repoNames.json").then( id2t => {
        id2Title = id2t;
    }).then( _gi => d3.json("data/summary.json")
    ).then(function(data){
        space = data.space;
        loadSpace(space, []);
        const tsptIds = Object.keys(data.transcripts);
        tsptIds.forEach( id => {
            dataPoints[id] = data.transcripts[id];
            dataPoints[id].id = id;
            const node = {
                data: dataPoints[id],
                x:SIZES.grid.cx,
                y:SIZES.grid.cy
            };
            nodes.push( node );
        });
    });
}

function makeCoordinateGetter( crdStr ) {
    const crd = crdStr.split("/");
    return function( value ) {
        for ( let x=0; x<crd.length; x++ ) {
            value = value[crd[x]];
        }
        return value;
    }
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

function menuSelectionChanged( aKey, aMenu ) {
    if ( ! initComplete ) return;
    readMenuStates(aKey, aMenu);
    
    if ( aKey === "color" ) {
        dataPointGs.selectAll("circle").transition().duration(500).style("fill",dataPointColor);
        return; // no need to move any point.
    }

    if ( aKey === "size" ) {
        dataPointGs.selectAll("circle").transition().duration(500).attr("r",dataPointSize);
    }
    if ( simulation ) {
        simulation.stop();
    }
    createSimulation();
}

function readMenuStates(aKey, aMenu) {

    // Forces
    VIEW_CRD.x.values = dim2coords[menus.x.value];
    VIEW_CRD.x.force = makeScreenLocationForce(menus.x.value, SIZES.grid.w, false);
    
    VIEW_CRD.y.values = dim2coords[menus.y.value];
    VIEW_CRD.y.force = makeScreenLocationForce(menus.y.value, SIZES.grid.h, true);
    
    // Visual properties
    VIEW_CRD.color.values = (menus.color.value!=="") ? dim2coords[menus.color.value] : [];
    VIEW_CRD.color.getCrd = (menus.color.value!=="") ? makeCoordinateGetter(menus.color.value) : null;
    
    VIEW_CRD.size.values = (menus.size.value!=="") ? dim2coords[menus.size.value] : [];
    VIEW_CRD.size.force = (menus.size.value!=="") ? makeDataPointSizeFunction(menus.size.value) : null;

    // axes
    if ( aKey === "x" || aKey==="y" ) {
        updateAxis(aKey);
    }
    
}

function updateAxis(axisName) {
    const crdText = menus[axisName].options[menus[axisName].selectedIndex].textContent;
    document.getElementById(axisName+"Label").innerText = crdText;
    const crdEmt = document.getElementById(axisName + "Crds");
    Utils.clear(crdEmt);
    let crdValues = dim2coords[menus[axisName].value];
    if ( axisName === "y" ) {
        crdValues = crdValues.slice();
        crdValues.reverse();
    }
    crdValues.forEach( v => {
        const emt = document.createElement("li");
        emt.innerText = v.replaceAll(/([A-Z])/g, " $1");
        crdEmt.append(emt);
    })
}

function loadSpace(slot, stack) {
    if ( slot.length ) {
        // Array - we've reached an actual dimension
        const optionValue = stack.join("/");
        const optionText = stack[stack.length-1];
        
        Object.keys(menus).forEach(k =>{
            const m = menus[k];
            const opt = document.createElement("option");
            opt.value = optionValue;
            opt.textContent = optionText.replaceAll(/([A-Z])/g, " $1");
            m.appendChild(opt);
        });
        dim2coords[optionValue] = slot;

    } else {
        // Object - expand the dimensions recursively
        Object.keys(slot).forEach( k => {
            stack.push(k);
            loadSpace( slot[k], stack );
            stack.pop();
        });
    }
}

function drawGraph(){
    // updateAxes();
    // addAxes();
    addDataPoints();
}

function addAxes() {

}

function addDataPoints() {
    dataPointGs = vizEmts.dataPoints.selectAll("g")
        .data( nodes, BY_ID )
        .enter()
        .append("g")
        .attr("id", BY_ID)
        .attr("class", "repo")
        .attr("transform", d=>"translate(" + d.x + "," + d.y + ")");

    dataPointGs.append("circle").attr("cx", 0).attr("cy", 0).attr("r", dataPointSize).style("fill", COLORS.default );
    dataPointGs.append("text").attr("x", 0).attr("y", 0)
            .attr("text-anchor","middle").attr("dy","0.35em")
            .text( d => id2Title[d.data.id].title );
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

    loadData().then( ()=>{
        Object.keys(menus).forEach(k =>{
            menus[k].addEventListener("change", ()=>menuSelectionChanged(k, menus[k]) );
        });
        drawGraph();
    }).then(()=>{
        menus.y.selectedIndex=1;
        menus.color.selectedIndex=0;
        menus.size.selectedIndex=0;
        updateAxis("x");
        updateAxis("y");
        readMenuStates();
        initComplete = true;
        createSimulation();
    });
}
