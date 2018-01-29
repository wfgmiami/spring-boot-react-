import React, { Component } from 'react';
import ReactDataGrid from 'react-data-grid';

class MuniList extends Component{
    constructor(props){
        super(props);
        this.state = {
            munis: []
        }

		this._columns = [
			{ key: 'cusip', name: 'Cusip', resizable: true },
			{ key: 'price', name: 'Price', resizable: true },
			{ key: 'state', name: 'State', resizable: true },
			{ key: 'ytw', name: 'Yield To Worst', resizable: true },
			{ key: 'rating', name: 'Rating', resizable: true },
			{ key: 'sector', name: 'Sector', resizable: true },
			{ key: 'coupon', name: 'Coupon', resizable: true },
			{ key: 'maturity', name: 'Maturity', resizable: true },
		]

		this.rowGetter = this.rowGetter.bind(this);
    }

    componentWillReceiveProps( nextProps ){
		if( nextProps.munis !== this.state.munis ){
			this.setState( { munis: nextProps.munis } );
		}
	}

	rowGetter( i ){
		return this.state.munis[i];
	}
	
    render(){
        const { munis } = this.state;
        const total = this.state.munis.length;
        const headerText = "Available Muni Bonds";

        return (
            <div>

            <ReactDataGrid
                columns={ this._columns }
                rowGetter = { this.rowGetter }
                rowsCount = { total }
                minHeight = { 500 }
                />
            </div>
        );
    }
}

export default MuniList;