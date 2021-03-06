import React, { Component } from 'react';
import './css/index.css';
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
    //    fetch("http://10.3.160.199:8080/app2")
        fetch("http://192.168.1.126:8080/app2")
        .then( response => response.json() )
        .then( data => this.setState( { munis: data } ) )
    }

    filterMaturity( maturityRange ){
        this.setState( { maturityRange } )
    }

    createLadder( investedAmount ){
//        let url = 'http://localhost:8080/app2/buckets';
    //    let url = "http://10.3.160.199:8080/app2/buckets";
        let url = "http://192.168.1.126:8080/app2/buckets";
//        console.log('.............this.state', this.state)
        this.setState({ investedAmount });
        let filter = Object.assign( {}, this.state.maturityRange, { investedAmount });

        axios.get(url, { params: filter })
            .then( response => response.data )
            .then( munis => {
				let allocRating = munis[0][1];
				let allocState = munis[0][2];
				let allocSector = munis[0][3];
				let allocSectorByState = munis[1][0];
				let averageRating = munis[2]["AverageRating"];
				let medianRating = munis[2]["MedianRating"];
				let allocatedData = munis[3];
				let aAndBelow = allocRating['aAndBelow'];
                let summary = { allocSector, allocState, allocRating };
                console.log('FINAL.....summary, allocatedData----', summary, allocatedData, allocSectorByState);

                const bucketsSummary = this.createSummary( summary, allocSectorByState );
                const bucketsByRows = this.createRows( allocatedData, averageRating, medianRating, aAndBelow );
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
		let setHeading = false;
		let heading = [];
		let idx = 0;
		const columnFields = [ 'portfolioSummary', 'dollarAllocated', 'percentageAllocated', 'rule', 'group' ];
debugger;
		groups.forEach( alloc => {
			let fields = Object.keys( summary[alloc] );
			let group = alloc;
			if( alloc === "allocSector" ){
				heading.push("SECTOR BREAKDOWN");
			}else if( alloc === "allocState" ){
				heading.push("STATE BREAKDOWN");
			}else if( alloc === 'allocRating' ){
				heading.push("SECTORS IN STATE BREAKDOWN");
			}
		});

		groups.forEach( alloc => {
			let fields = Object.keys( summary[alloc] );
			let group = alloc;
			rowObj[columnFields[0]] = heading[idx++];
			bucketsSummary.push( rowObj );
			rowObj = {};

			fields.forEach( field => {
				if(field === "Cash" || field === 'aAndBelow') return;
				if(setHeading){
					rowObj[columnFields[0]] = heading;
					bucketsSummary.push( rowObj );
					setHeading = false;
				}else{
					rowObj[columnFields[0]] = field;
					rowObj[columnFields[1]] = '$' + ( summary[alloc][field] ).toLocaleString();
					rowObj[columnFields[2]] = Number( ( ( summary[alloc][field] * 1 / this.state.investedAmount *  1 ) * 100 ).toFixed(2) ) + '%';
					rowObj[columnFields[4]] = group;

					if( field === 'Health Care' ){
						rowObj[columnFields[3]] = '<= 12%';
					// }else if( field === 'aAndBelow' ){
					// 	rowObj[columnFields[0]] = "A AND BELOW RATING";
					// 	rowObj[columnFields[3]] = '<= 30%';
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
					}
				}
				rowObj = {};
			})
		})

		let obj = {};
		let arr = [];

		Object.keys( allocSectorByState ).forEach( state => {
		    // let keep = false;
			// obj['portfolioSummary'] = state;
			// arr.push(obj);
			// obj = {};
			Object.keys( allocSectorByState[state] ).forEach( sector => {
				obj['portfolioSummary'] = state + ' - ' + sector;
				obj['dollarAllocated'] = allocSectorByState[state][sector].toLocaleString();
				obj['percentageAllocated'] =  Number( ( ( allocSectorByState[state][sector] * 1 / this.state.investedAmount *  1 ) * 100 ).toFixed(2) ) + '%';
				obj['rule'] = '<= 10%';
				if(obj['dollarAllocated'] !== '0') {
				    arr.push(obj);
				    // keep = true;
				}
				obj = {};
			})

			// if(!keep) arr.splice(-1,1);

		})

		return bucketsSummary.concat(arr);
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

	createRows( objBuckets, averageRating, medianRating, aAndBelow ){

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
		let avgCurrentYield = 0;
		let tdRange = [];
		let portfolioSummary = [];
		let minTdDate = 0;
		let maxTdDate = 0;
		let tradeDateRange = '';
        let totalInvested = 0;
		let percentCash = 0;

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

		bucketsByRows.push( totalByBucket );
		bucketsByRows.push({});
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

							avgCurrentYield += ( bond.coupon / bond.price ) * bond.investAmt;
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

		avgCurrentYield = Number( avgCurrentYield * 100 / ( this.state.investedAmount - cashPosition ) ).toFixed(2);
		if( isNaN( avgCurrentYield ) ) avgCurrentYield = '';
		else avgCurrentYield = avgCurrentYield + '%';
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
		percentCash = Number((cashPosition / this.state.investedAmount * 100).toFixed(2)).toLocaleString();
		cashPosition = '$' +  Number(cashPosition.toFixed(2)).toLocaleString() + " | " + percentCash + "%";

		aAndBelow = Number((aAndBelow / this.state.investedAmount * 100).toFixed(2)).toLocaleString() + "%";
		if(!aAndBelow) aAndBelow = "0%";

		portfolioSummary.push( { avgPrice, avgCoupon, yieldToWorst: avgYtw, modifiedDuration: avgModDuration, effectiveDuration: avgEffDuration, 
			cash: cashPosition, numberOfBonds: numBonds, portfolioSize, avgCurrentYield, aAndBelow, averageRating, medianRating, tradeDateRange } );

		this.setState( { portfolioSummary } );
		// bucketsByRows.push( totalByBucket );
		return bucketsByRows;
	}

    render() {
        const munis = [...this.state.munis];
//        console.log('app state.......', this.state);
        return (
          <div className="App">
            <div className="container-fluid">
                <Nav filterMaturity = { this.filterMaturity } createLadder = { this.createLadder } />
                <div style={{ marginTop: '100px' }}>
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
