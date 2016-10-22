
    // Global state that doesn't fit in React (because of React's
    // asynchronous way of updating setState
    var globalState = {
        mouseDown: false,
        mouseDownX: 0,
        mouseDownY: 0
    }

    var buttonStyle = {
        WebkitBorderRadius: 7,
        MozBorderRadius: 7,
        borderRadius: '7px',
        fontFamily: 'Arial',
        color: '#ffffff',
        fontSize: '20px',
        background: '#3498db',
        padding: '10px 20px 10px 20px',
        textDecoration: 'none'
    }

    var buttonStyleHover = {
        WebkitBorderRadius: 7,
        MozBorderRadius: 7,
        borderRadius: '7px',
        fontFamily: 'Arial',
        color: '#ffffff',
        fontSize: '20px',
        padding: '10px 20px 10px 20px',

        background: '#3cb0fd',
        textDecoration: 'none'
    }

    function House(x, y, type) {
        this.x = x
        this.y = y
        this.type = type
    }

    var ZoomInButton = React.createClass({
        getInitialState: function() {
            return {
                style: buttonStyle
            }
        },

        onMouseOver: function() {
            this.setState({
                style: buttonStyleHover
            });
        },

        onMouseOut: function() {
            this.setState({
                style: buttonStyle
            });
        },

        zoomIn: function(e) {
            this.props.zoomIn();

            e.stopPropagation();
        },

        render: function() {
            return (
                    <span className="ZoomInButton"
                          onClick={this.zoomIn}
                          onMouseOver={this.onMouseOver}
                          onMouseOut={this.onMouseOut}
                          style={this.state.style}>
                        In
                    </span>
            );
        }
    });

    var ZoomOutButton = React.createClass({
        getInitialState: function() {
            return {
                style: buttonStyle
            }
        },

        onMouseOver: function() {
            this.setState({
                style: buttonStyleHover
            });
        },

        onMouseOut: function() {
            this.setState({
                style: buttonStyle
            });
        },

        zoomOut: function(e) {
            this.props.zoomOut();

            e.stopPropagation();
        },

        render: function() {
            var style = {
                userSelect: 'none',
	        backgroundColor: 'BlueViolet',
	        color: 'white',
	        zIndex: 2
            }

            return (
                    <span className="ZoomOutButton"
                          style={style}
                          onClick={this.zoomOut}
                          onMouseOver={this.onMouseOver}
                          onMouseOut={this.onMouseOut}
                          style={this.state.style}>
                        Out
                    </span>
            );
        }
    });

    var InfoLabel = React.createClass({
        render: function() {
            var style = {
		userSelect: 'none',
		backgroundColor: 'CadetBlue',
		color: 'white',
		zIndex: 1
            }

            return (<span className="InfoLabel"
                         style={style}>
                        [{this.props.translateX}, {this.props.translateY}], 
                    mouse down: {this.props.mouseDown.toString()}
                    </span>
                   );
        }
    });

    var Toolbar = React.createClass({
        render: function() {
            return (<div className="Toolbar">
                        <InfoLabel
                            translateX={this.props.translateX}
                            translateY={this.props.translateY}
                            mouseDown={this.props.mouseDown}
                        />

                        <ZoomInButton zoomIn={this.props.zoomIn} />
                        <ZoomOutButton zoomOut={this.props.zoomOut} />

                    </div>
            );
        }

    });

    var HouseTitle = React.createClass({
	render: function() {
	    var style = {
		userSelect: 'none',
		position: 'absolute',
		top: this.props.y*this.props.scale + this.props.translateY - 20,
		left: this.props.x*this.props.scale + this.props.translateX,
		color: 'white'
	    }

	    return (
		    <div style={style}>
		    {this.props.type}
		    </div>
	    );
	}
    });

    var Worker = React.createClass({
        render: function() {
            var style = {
	        userSelect: 'none',
	        position: 'absolute',
	        top: this.props.y*this.props.scale + this.props.translateY,
	        left: this.props.x*this.props.scale + this.props.translateX
            }

            return (
	            <div style={style} className="HouseTexture">
		    <img
                        className="houseTexture"
                        src="walking-guy.jpg"
                        width="20"
                        height="20" />
		    </div>
            );
        }
    });


    var HouseTexture = React.createClass({
        render: function() {

	    var houseStyle = {
		userSelect: 'none',
		position: 'absolute',
		top: this.props.y*this.props.scale + this.props.translateY,
		left: this.props.x*this.props.scale + this.props.translateX
	    }

            return (
	        <div style={houseStyle} className="HouseTexture">
		    <img
		        className="houseTexture"
		        src="house.jpg"
		        width="50"
		        height="50"
		    />
		</div>
            );
        }
    });

    var Canvas = React.createClass({
        getInitialState: function() {
	    return {
		houses: [],
                workers: [],
		translateX: 0,
		translateY: 0,
		scale: 5
	    };
	},

        zoomIn: function() {
            console.info("Zooming in, from " + this.state.scale)
            if (this.state.scale < 20) {
                console.info("zooming...");
                this.setState({
		    houses: this.state.houses,
                    workers: this.state.workers,
		    translateX: this.state.translateX,
		    translateY: this.state.translateY,
		    scale: this.state.scale + 1
                });
            }
        },

        zoomOut: function() {
            console.info("Zooming out, from " + this.state.scale)
            if (this.state.scale > 1) {
                this.setState({
	            houses: this.state.houses,
                    workers: this.state.workers,
		    translateX: this.state.translateX,
		    translateY: this.state.translateY,
		    scale: this.state.scale - 1
                });
            }
        },
        
	onMouseDown: function(event) {
            globalState.mouseDown = true;
            globalState.mouseDownX = event.pageX;
            globalState.mouseDownY = event.pageY;

	    this.setState({
		houses: this.state.houses,
                workers: this.state.workers,
		translateX: this.state.translateX,
		translateY: this.state.translateY,
		scale: this.state.scale
	    });
	},

	onMouseMove: function(event) {
	    if (globalState.mouseDown) {
		var deltaX = (event.pageX - globalState.mouseDownX) / 4.0;
		var deltaY = (event.pageY - globalState.mouseDownY) / 4.0;

		this.setState({
		    houses: this.state.houses,
                    workers: this.state.workers,
		    translateX: this.state.translateX - deltaX,
		    translateY: this.state.translateY - deltaY,
		    scale: this.state.scale
		});
	    }
	},

	onMouseUp: function(event) {
            globalState.mouseDown = false
        },

	componentDidMount: function() {
	    var updateGameFromServer = function() {

		$.ajax({
		    url: this.props.url + "/viewForPlayer?player=0",
		    dataType: 'json',
		    cache: false,

		    success: function(data) {
                        console.info(JSON.stringify(data));
			this.setState({
			    houses: data.houses,
                            workers: data.workers,
			    translateX: this.state.translateX,
			    translateY: this.state.translateY,
			    scale: this.state.scale
			});

			setTimeout(function() {
			    updateGameFromServer();
			}, 100);

		    }.bind(this),

		    error: function(xhr, status, err) {
			console.error("error sucks");
			console.error(xhr);
			console.error(status);
			console.error(err);

			setTimeout(function() {
			    updateGameFromServer(this);
			}, 300);
		    }.bind(this)
		})
	    }.bind(this)


            // Move the workers locally to cover for periods between updates
            var moveWorkersLocally = function() {

                // Move workers
                movedWorkers = this.state.workers.map(
                    function(w) {
                        
                    });

                

                // Schedule the next run
                setTimeout(function() {
                    moveWorkersLocally(this);
                }, 20);
            }

	    updateGameFromServer();
	},

        render: function() {
	    var canvasStyle = {
		userSelect: 'none',
		backgroundColor: '#000000',
		height: '100vh',
		width: '100%',
		zIndex: 0
	    }

            return (
		    <div className="Canvas"
		         style={canvasStyle}
                         id="gameCanvas"
                         onMouseDown={this.onMouseDown}
                         onMouseMove={this.onMouseMove}
                         onMouseUp={this.onMouseUp} >

                        {this.state.houses.map(function(house, i) {
			    return <HouseTexture
		               type={house.type}
                               x={house.x}
                               y={house.y}
                               key={i}
                               translateX={this.state.translateX}
                               translateY={this.state.translateY}
                               scale={this.state.scale}/>;
			}, this)}

                        {this.state.houses.map(function(house, i) {
			    return <HouseTitle
                               type={house.type}
                               x={house.x}
                               y={house.y}
                               translateX={this.state.translateX}
                               translateY={this.state.translateY}
                               key={i}
                               scale={this.state.scale} />;
			}, this)}

                        {this.state.workers.filter(w => !w.inside).map(function(worker, i) {
                            return <Worker
                            type={worker.type}
                            x={worker.x}
                            y={worker.y}
                            translateX={this.state.translateX}
                            translateY={this.state.translateY}
                            key={i}
                            scale={this.state.scale} />;
                        }, this)}
                
                        <Toolbar
                            translateX={this.state.translateX}
                            translateY={this.state.translateY}
                            mouseDown={globalState.mouseDown}
                            zoomIn={this.zoomIn}
                            zoomOut={this.zoomOut}
                    />
		    </div>
            );
        }
    });
ReactDOM.render(
    <Canvas url="http://localhost:8080" />,
    document.getElementById('content')
);
