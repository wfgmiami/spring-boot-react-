import React, { Component } from 'react';
import logo from './logo.svg';
import './App.css';
import MuniList from './MuniList';
import Constraint from './Constraint';
import BucketSummaryPlaceholder from './BucketSummaryPlaceholder';
import Nav from './Nav';
import axios from 'axios';

class App extends Component {
    constructor(props){
        super(props);

        this.state = {
            munis:[],
            maturityRange: { min: 1, max: 5 }
        }

        this.filterMaturity = this.filterMaturity.bind(this);
        this.createLadder = this.createLadder.bind(this);
    }

    componentDidMount(){
        fetch("http://localhost:8080")
        .then( response => response.json() )
        .then( data => this.setState( { munis: data } ) )
    }

    filterMaturity( maturityRange ){
        this.setState( { maturityRange } )
    }

    createLadder( investedAmount ){
        let url = 'http://localhost:8080/buckets';
//        console.log('.............this.state', this.state)

        let filter = Object.assign( {}, this.state.maturityRange, { investedAmount });
        console.log('....filter', filter);

        axios.get(url, { params: filter })
            .then( response => response.data )
            .then( munis => console.log("from axios......", munis))

    }

    render() {
        const munis = [...this.state.munis];
        console.log('app state.......', this.state);
        return (
          <div className="App">
            <div className="container-fluid">
                <Nav filterMaturity = { this.filterMaturity } createLadder = { this.createLadder } />
                <div style={{ marginTop: '135px' }} className="row">

                    <div className="col-sm-8">
                        <BucketSummaryPlaceholder />
                    </div>
                    <div className="col-sm-4">
                        <Constraint />
                        <MuniList munis = { munis } />
                    </div>
                </div>
             </div>
          </div>
        );
    }
}

export default App;
