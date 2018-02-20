import React, { Component } from 'react';
import logo from './logo.svg';
import './App.css';
import MuniList from './MuniList';
import Constraint from './Constraint';
import BucketAllocation from './BucketAllocation';
import BucketSummaryPlaceholder from './BucketSummaryPlaceholder';
import PortfolioSummary from './PortfolioSummary';
import BucketSummary from './BucketSummary';
import Nav from './Nav';
import axios from 'axios';

class Versiontwo extends Component {
    constructor(props){
        super(props);

        this.state = {
            munis:[],
            maturityRange: { min: 1, max: 5 },
            bucketsByRows: [],
            columns: [],
            bucketsSummary: [],
            portfolioSummary: [],
            investedAmount: 1000000
        }

        this.filterMaturity = this.filterMaturity.bind(this);
        this.createLadder = this.createLadder.bind(this);
        this.createRows = this.createRows.bind(this);
        this.createColumns = this.createColumns.bind(this);
        this.createSummary = this.createSummary.bind(this);
    }

    componentDidMount(){
//        fetch("http://localhost:8080/app2")
//        fetch("http://10.3.160.199:8080/app2")
        fetch("http://192.168.1.126:8080/app2")
        .then( response => response.json() )
        .then( data => this.setState( { munis: data } ) )
    }

    filterMaturity( maturityRange ){
        this.setState( { maturityRange } )
    }

    createLadder( investedAmount ){
//        let url = 'http://localhost:8080/app2/buckets';
//        let url = "http://10.3.160.199:8080/app2/buckets";
        let url = "http://192.168.1.126:8080/app2/buckets";
//        console.log('.............this.state', this.state)
        this.setState({ investedAmount });
        let filter = Object.assign( {}, this.state.maturityRange, { investedAmount });

        axios.get(url, { params: filter })
            .then( response => response.data )
            .then( munis => {
                let allocatedData = munis[5];
                let allocSector = munis[3];
                let allocState = munis[2];
                let allocRating = munis[1];
                let allocSectorByState = munis[4];
                let summary = { allocSector, allocState, allocRating };
                console.log('FINAL.....summary, allocatedData----', summary, allocatedData, allocSectorByState);

                const bucketsSummary = this.createSummary( summary, allocSectorByState );
                const bucketsByRows = this.createRows( allocatedData );
                const columns = this.createColumns();
                this.setState({ columns });
                this.setState({ bucketsByRows });
                this.setState({ bucketsSummary });
           })
    }

    createSummary( summary, allocSectorByState ){
		let groups = Object.keys( summary );
		let bucketsSummary = [];
		let rowObj = {};
		let arrangedPortfolioSummary = [];
		const columnFields = [ 'portfolioSummary', 'dollarAllocated', 'percentageAllocated', 'rule', 'group' ];

		groups.forEach( alloc => {
			let fields = Object.keys( summary[alloc] );
			let group = alloc;
			fields.forEach( field => {

				rowObj[columnFields[0]] = field;
				rowObj[columnFields[1]] = '$' + ( summary[alloc][field] ).toLocaleString();
				rowObj[columnFields[2]] = Number( ( ( summary[alloc][field] * 1 / this.state.investedAmount *  1 ) * 100 ).toFixed(2) ) + '%';
				rowObj[columnFields[4]] = group;

				if( field === 'Health Care' ){
					rowObj[columnFields[3]] = '<= 12%';
				}else if( field === 'aAndBelow' ){
					rowObj[columnFields[3]] = '<= 30%';
				}else if( group === 'allocSector' && field !== 'Cash' ){
					rowObj[columnFields[3]] = '<= 30%';
				}else if( group === 'allocState' ){
					rowObj[columnFields[3]] = '<= 20%';
				}else if( field === 'NY' ){
					rowObj[columnFields[3]] = '<= 20%';
				}else if( field === 'CA' ){
					rowObj[columnFields[3]] = '<= 20%';
				}
				if( rowObj[columnFields[1]] !== '$0' ){
					bucketsSummary.push( rowObj );
					rowObj = {};
				}
			})
		})

		let sectorObj = 0;
		let ratingObj = 0;
		let stateObj = 0;

		const arrLen = bucketsSummary.length - 1;
		for( let i = 0; i < arrLen + 1; i++ ){
			if( bucketsSummary[i].group === 'allocSector' ){
				sectorObj++;
			}else if( bucketsSummary[i].group === 'allocState' ){
				stateObj++;
			}else if( bucketsSummary[i].group === 'allocRating') {
				ratingObj++;
			}
		}
		let stateStart = sectorObj + ratingObj - 1;
		let stateStartRest = stateStart + 2;
		let startRating = sectorObj - 1;

		arrangedPortfolioSummary = bucketsSummary.map( ( obj, index ) => {
			let indexedObj = {};
			let startIndex = 1;
			indexedObj = Object.assign( obj, { index: index } );

			if( obj.group === 'allocSector' && obj.portfolioSummary === 'Health Care' ){
				indexedObj = { id: startIndex, obj };
			}else if( obj.group === 'allocSector' && obj.portfolioSummary !== 'Cash' ){
				indexedObj = { id: ++startIndex, obj }
			}else if( obj.group === 'allocState' && obj.portfolioSummary === 'CA' ){
				indexedObj = { id: stateStart + 1, obj };
			}else if( obj.group === 'allocState' && obj.portfolioSummary === 'NY' ){
				indexedObj = { id: stateStart, obj }
			}else if( obj.group === 'allocState' ){
				indexedObj = { id: ++stateStartRest, obj };
			}else if( obj.portfolioSummary === 'aAndBelow' ){
				obj.portfolioSummary = 'A Rated and Below';
				indexedObj = { id: startRating, obj }
			}else if( obj.portfolioSummary === 'Cash' ){
				indexedObj = { id: 0, obj }
			}

			return indexedObj;
		})

		console.log('..................arrangedPortfolioSummary', arrangedPortfolioSummary);
		arrangedPortfolioSummary.sort( function(a, b){ return a.id - b.id } );
		let result = arrangedPortfolioSummary.map( obj => bucketsSummary[obj.obj.index] );
		let obj = {};
		let arr = [];

		Object.keys( allocSectorByState ).forEach( state => {
			obj['portfolioSummary'] = state;
			arr.push(obj);
			obj = {};
			Object.keys( allocSectorByState[state] ).forEach( sector => {
				obj['portfolioSummary'] = sector;
				obj['dollarAllocated'] = allocSectorByState[state][sector].toLocaleString();
				obj['percentageAllocated'] =  Number( ( ( allocSectorByState[state][sector] * 1 / this.state.investedAmount *  1 ) * 100 ).toFixed(2) ) + '%';
//				allocSectorByState[state][sector].toLocaleString();
				obj['rule'] = '<= 10%';
				arr.push(obj);
				obj = {};
			})

		})
		//let result = arrangedPortfolioSummary.concat(arr);
		return result.concat(arr);

	}

	createColumns(){
		let columns = [];
        let columnsYears = [];
        let min = this.state.maturityRange.min * 1;
        let max = this.state.maturityRange.max * 1;
        for(let i = min; i <= max; i++){
            columnsYears.push(i);
        }

    	for( let i = 0; i < columnsYears.length; i++ ){
			columns.push( { key: (columnsYears[i]).toString(),
				name: ( columnsYears[i] ), resizable: true } )
		}
		return columns;
	}

	createRows( objBuckets ){

		const buckets = Object.keys( objBuckets );
		const numBuckets = buckets.length;
		const portfolioSize = '$' + parseInt(this.state.investedAmount).toLocaleString();

		let lenBucket = [];
		let bucketsByRows = [];
		let maxBondsInBucket = 0;
		let rowsPerBond = 4;
		let bond = {};
		let row = {};
		let totalByBucket = {};
		let totalInBucket = 0;
		let bucketIndex = buckets[0];
		let numBonds = 0;
		let cashPosition = 0;
		let avgEffDuration = 0;
		let avgModDuration = 0;
		let avgPrice = 0;
		let avgCoupon = 0;
		let avgYtw = 0;
		let tdRange = [];
		let portfolioSummary = [];
		let minTdDate = 0;
		let maxTdDate = 0;
		let tradeDateRange = '';
        let totalInvested = 0;

		buckets.forEach( bucket => {
				lenBucket.push( objBuckets[bucket].length );
				numBonds += objBuckets[bucket].length;

				for( let j = 0; j < objBuckets[bucket].length; j++ ){
					totalInBucket += objBuckets[bucket][j].investAmt;
				}

				let percBucket =  Number( ( totalInBucket / this.state.investedAmount * 100 ) ).toFixed(2).toLocaleString();
				totalByBucket[bucket] = '$' + totalInBucket.toLocaleString() + ', ' + percBucket + '%';
				totalInBucket = 0;

		})

		maxBondsInBucket = Math.max(...lenBucket);
		console.log('.....totalByBucket,maxBondInBucket, rowsPerBond, bucketIndex, numBuckets, numBonds', totalByBucket,maxBondsInBucket, rowsPerBond, bucketIndex, numBuckets, objBuckets, numBonds);
		for(let i = 0; i < maxBondsInBucket; i++){
			for(let j = 0; j < rowsPerBond; j++){
				for(let k = bucketIndex; k < numBuckets + bucketIndex*1; k++){

					bond = objBuckets[k][i];

					if( bond ){
						if( j === 0 ){
							if( bond.cusip === 'Cash' ){
								row[(k).toString()] = bond.cusip + ': $' + bond.investAmt.toLocaleString();
								cashPosition += bond.investAmt;
							}else{
								row[(k).toString()] = bond.cusip + ', ' + bond.coupon + '%, ' + bond.maturityDate.substring(0,6) + bond.maturityDate.substring(8);
							}
						}else if( j === 1 && bond.cusip !== 'Cash' ){
							row[(k).toString()] = bond.state + ', ' + bond.sector + ', ' + bond.rating;

							avgEffDuration += bond.effDur * bond.investAmt;
							avgModDuration += bond.modDur * bond.investAmt;
							avgYtw += bond.yieldToWorst * bond.investAmt;
							avgPrice += bond.price * bond.investAmt;
							avgCoupon += bond.coupon * bond.investAmt
							tdRange.push( new Date ( bond.latestTraded ).getTime() );
						}else if( j === 2 && bond.cusip !== 'Cash' ){
								row[(k).toString()] = bond.latestTraded + ', ' + bond.price;
						}else if( j === 3 && bond.cusip !== 'Cash' ){
						    totalInvested += bond.investAmt;
							let par = Number( (bond.investAmt / ( bond.price / 100 ) ).toFixed(0) / 1000 ).toLocaleString() + 'k';
							let percPos = Number( ( bond.investAmt / this.state.investedAmount * 100 ) ).toFixed(2).toLocaleString();
							row[(k).toString()] = '$' + bond.investAmt.toLocaleString() + ', ' + par + ', ' + percPos + "%";
						}
					}

				}
				if( Object.keys( row ).length !== 0 ){
					bucketsByRows.push( row );
					row = {};
				}
			}
			bucketsByRows.push( {} );
		}
        if(cashPosition == 0) cashPosition = this.state.investedAmount - totalInvested;
		minTdDate = Math.min( ...tdRange );
		maxTdDate = Math.max( ...tdRange );
		minTdDate = new Date( minTdDate ).toLocaleString().split(',')[0];
		maxTdDate = new Date( maxTdDate ).toLocaleString().split(',')[0];
		if( minTdDate === 'Invalid Date' || maxTdDate === 'Invalid Date' ){
			tradeDateRange = ''
		}else{
			tradeDateRange = minTdDate + ' - ' + maxTdDate;
		}

		avgEffDuration = Number( avgEffDuration / ( this.state.investedAmount - cashPosition ) ).toFixed(2);
		if( isNaN( avgEffDuration ) ) avgEffDuration = '';
		avgModDuration = Number( avgModDuration / ( this.state.investedAmount - cashPosition ) ).toFixed(2);
		if( isNaN( avgModDuration ) ) avgModDuration = '';
		avgYtw = Number( avgYtw / ( this.state.investedAmount - cashPosition ) ).toFixed(2);
		if( isNaN( avgYtw ) ) avgYtw = '';
		else avgYtw = avgYtw + '%';
		avgCoupon = Number( avgCoupon / ( this.state.investedAmount - cashPosition ) ).toFixed(2);
		if( isNaN( avgCoupon ) ) avgCoupon = '';
		else avgCoupon = avgCoupon + '%';
		avgPrice = Number( avgPrice / ( this.state.investedAmount - cashPosition ) ).toFixed(2);
		if( isNaN( avgPrice ) ) avgPrice = '';
		cashPosition = '$' +  Number(cashPosition.toFixed(2)).toLocaleString();

		portfolioSummary.push( { avgPrice, avgCoupon, yieldToWorst: avgYtw, modifiedDuration: avgModDuration, effectiveDuration: avgEffDuration, cash: cashPosition, numberOfBonds: numBonds, portfolioSize, tradeDateRange } );

		this.setState( { portfolioSummary } );
		bucketsByRows.push( totalByBucket );
		return bucketsByRows;
	}

    render() {
        const munis = [...this.state.munis];
//        console.log('app state.......', this.state);
        return (
          <div className="App">
            <div className="container-fluid">
                <Nav filterMaturity = { this.filterMaturity } createLadder = { this.createLadder } />
                <div style={{ marginTop: '135px' }} className="row">
                <PortfolioSummary portfolioSummary = { this.state.portfolioSummary } />
            	{ this.state.bucketsByRows.length !== 0 ?
                    <div className="col-sm-12">
                        <BucketAllocation columns = { this.state.columns } bucketsByRows = { this.state.bucketsByRows }/>
                        <BucketSummary bucketsSummary = { this.state.bucketsSummary } />
                        <div>&nbsp;</div>
                    </div>:
                    <div className="col-sm-12">
                        <BucketSummaryPlaceholder />
            		</div> }

                    <div className="col-sm-12">
                        <Constraint />
                    </div>
                </div>
             </div>
          </div>
        );
    }
}

export default Versiontwo;
