/* jshint esversion:6 */

const Utils = (function(){
    function ready(fn) {
        if (document.attachEvent ? document.readyState === "complete" : document.readyState !== "loading"){
            fn();
        } else {
            document.addEventListener('DOMContentLoaded', fn);
        }
    }

    function makeCounter(initialCount, callbackOnZero) {
        var counter = initialCount;
        return {
            countUp: function () {
                counter++;
            },
            countDown: function () {
                counter--;
                if (counter === 0) {
                    callbackOnZero();
                }
            },
            getCount: function () {
                return counter;
            }
        };
    }

    function get( url, dataCallback ) {
        var properties = {
            method: "GET",
            redirect: "follow",
            credentials: "same-origin",
            headers: new Headers()
        };
        var request = new Request(url, properties);

        fetch(request).then( function(res){
            if ( res.ok ) {
            return res.json();
        } else {
            console.error("Error GETting '" + url + "': %o (on return)", res );
        }
    }).then( function(json){dataCallback(json);} );
    }

    function clear(emt) { 
        while ( emt.children.item(0) ) {
            emt.children.item(0).remove();
        }
    }

    return {
        ready:ready,
        makeCounter:makeCounter,
        clear:clear,
        get:get
    };

})();