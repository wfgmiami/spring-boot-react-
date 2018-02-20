import React from 'react';

import AmountSlider from './AmountSlider';
import MaturitySlider from './MaturitySlider';

class Nav extends React.Component {

  constructor(props) {
    super(props);

    this.state = {
      collapsed: true
    }

    this.toggleNavbar = this.toggleNavbar.bind(this);
	  this.onMinAllocChange = this.onMinAllocChange.bind(this);
  }

  onMinAllocChange = (e) => {
	this.props.handleMinAllocChange(e.target.value);
  }

  toggleNavbar(){
    this.setState({
      collapsed: !this.state.collapsed
    })
  }

  render() {

    // const collapsed = this.state.collapsed;
    // const classOne = collapsed ? 'collapse navbar-collapse' : 'collapse navbar-collapse show';
    // const classTwo = collapsed ? 'navbar-toggle navbar-toggle-right collapsed' : 'navbar-toggle navbar-togler-right';

	  return (
      <div>
        <nav className="navbar navbar-inverse navbar-fixed-top">
          {/* <div className="container-fluid" style={{backgroundColor:"black"}}> */}
            <div className="row">

              <div className='col-md-2'>
                {/* <div className="navbar-header"> */}
                    {/* <button  onClick={ this.toggleNavbar } className={ `${classTwo}` } type="button" data-toggle="collapse" data-target="#navbarResponsive">
                      <span className="icon-bar" />
                      <span className="icon-bar" />
                      <span className="icon-bar" />
                    </button> */}
					<h4 style={{ marginLeft: '7px' }}><b><span style={{ fontSize: '22' }}>M</span>UNI <span style={{ fontSize: '22' }}>L</span>ADDER <span style={{ fontSize: '22' }}>A</span>LLOCATION</b></h4>
                {/* </div> */}
              </div>

              
              {/* <div className={ `${ classOne }` } id="navbarResponsive"> */}
                <div className="col-md-5">
                    <MaturitySlider filterMaturity = { this.props.filterMaturity }/>
                </div>

                <div className="col-md-5">
                  	<AmountSlider createLadder = { this.props.createLadder }/>
                </div>
              {/* </div> */}

            </div>
          {/* </div> */}
        </nav>
      </div>

    );
  }
}


export default Nav;